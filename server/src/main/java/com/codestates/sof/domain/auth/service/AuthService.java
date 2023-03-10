package com.codestates.sof.domain.auth.service;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.codestates.sof.domain.auth.entity.VerificationToken;
import com.codestates.sof.domain.auth.jwt.JwtTokenizer;
import com.codestates.sof.domain.auth.repository.VerificationTokenRepository;
import com.codestates.sof.domain.common.RandomPasswordGenerator;
import com.codestates.sof.domain.member.entity.Member;
import com.codestates.sof.domain.member.repository.MemberRepository;
import com.codestates.sof.global.error.dto.ExceptionCode;
import com.codestates.sof.global.error.exception.BusinessLogicException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class AuthService {
	private final VerificationTokenRepository verificationTokenRepository;
	private final MemberRepository memberRepository;
	private final EmailService emailService;
	private final PasswordEncoder passwordEncoder;
	private final RedisTemplate<String, String> redisTemplate;
	private final JwtTokenizer jwtTokenizer;

	public String generateToken(Member member, int expirationHour, VerificationToken.tokenType tokenType) {
		String token = UUID.randomUUID().toString();
		VerificationToken verificationToken = new VerificationToken();
		verificationToken.setToken(token);
		verificationToken.setMember(member);
		verificationToken.setExpiryDate(expirationHour);
		verificationToken.setTokenType(tokenType);

		verificationTokenRepository.save(verificationToken);

		return token;
	}

	public ResponseEntity<?> verifyAccount(String token) {
		Optional<VerificationToken> optionalToken = verificationTokenRepository.findByToken(token);
		VerificationToken verificationToken = optionalToken.orElseThrow(() -> {
			throw new BusinessLogicException(ExceptionCode.NOT_FOUND_TOKEN);
		});

		// token type??? ????????? ???????????????.
		if (verificationToken.getTokenType() != VerificationToken.tokenType.VERIFICATION) {
			throw new BusinessLogicException(ExceptionCode.INVALID_TOKEN);
		}

		Member member = findMemberByTokenEmail(verificationToken);

		// ????????? ?????? ????????? ???????????? ?????? ????????? ?????? ???????????? ????????????.
		if (verificationToken.isExpired()) {
			String newToken = generateToken(member, 60, VerificationToken.tokenType.VERIFICATION);
			emailService.sendActivationEmail(member, newToken);
			return new ResponseEntity<>("Token is expired. Check your email again.", HttpStatus.SERVICE_UNAVAILABLE);
		}

		member.setVerificationFlag(true);
		memberRepository.save(member);

		return new ResponseEntity<>("Account Activated Successfully", HttpStatus.OK);
	}

	public void resetPassword(String token) {
		Optional<VerificationToken> optionalToken = verificationTokenRepository.findByToken(token);
		VerificationToken verificationToken = optionalToken.orElseThrow(() -> {
			throw new BusinessLogicException(ExceptionCode.NOT_FOUND_TOKEN);
		});

		// token type??? ????????? ???????????????.
		if (verificationToken.getTokenType() != VerificationToken.tokenType.PASSWORD_RESET) {
			throw new BusinessLogicException(ExceptionCode.INVALID_TOKEN);
		}

		Member member = findMemberByTokenEmail(verificationToken);
		String generatePassword = RandomPasswordGenerator.generate(2, 2, 2, 2);

		member.setBeforeEncryptedPassword(member.getEncryptedPassword());
		member.setEncryptedPassword(passwordEncoder.encode(generatePassword));
		memberRepository.save(member);

		emailService.sendPasswordResetEmail(member, generatePassword);
	}

	private Member findMemberByTokenEmail(VerificationToken verificationToken) {
		String memberEmail = verificationToken.getMember().getEmail();
		Optional<Member> optionalMember = memberRepository.findByEmail(memberEmail);

		Member member = optionalMember.orElseThrow(() -> new BusinessLogicException(ExceptionCode.NOT_FOUND_MEMBER));
		if (member.getDeleteFlag()) {
			throw new BusinessLogicException(ExceptionCode.NOT_FOUND_MEMBER);
		}
		return member;
	}

	@Transactional
	public void logout(HttpServletRequest request) {
		// ????????? access token??? ????????? ??????
		String accessToken = request.getHeader("Authorization");
		if (accessToken == null || !accessToken.startsWith("Bearer ")) {
			throw new BusinessLogicException(ExceptionCode.INVALID_TOKEN);
		}

		String jws = accessToken.replace("Bearer ", "");
		String base64EncodedSecretKey = jwtTokenizer.encodeBase64SecretKey(jwtTokenizer.getSecretKey());
		Map<String, Object> claims = jwtTokenizer.getClaims(jws, base64EncodedSecretKey).getBody();

		// Redis?????? ?????? username??? ????????? refresh token??? ????????? ????????? ?????? ???, ?????? ?????? ??????
		if (redisTemplate.opsForValue().get("RTK:" + claims.get("username")) != null) {
			redisTemplate.delete("RTK:" + claims.get("username"));
		}

		// access token??? ?????? ????????? redis??? ?????? ????????? ?????????????????? ?????? blacklist ???????????? ???????????????.
		long expiration = (Long)claims.get("expiration") - new Date().getTime();
		redisTemplate.opsForValue().set(jws, "blacklist", expiration, TimeUnit.MILLISECONDS);

		SecurityContextHolder.clearContext();
	}
}