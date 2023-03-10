package com.codestates.sof.domain.answer.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.codestates.sof.domain.answer.dto.AnswerVoteDto;
import com.codestates.sof.domain.answer.entity.Answer;
import com.codestates.sof.domain.answer.entity.AnswerVote;
import com.codestates.sof.domain.answer.repository.AnswerRepository;
import com.codestates.sof.domain.answer.repository.AnswerVoteRepository;
import com.codestates.sof.domain.answer.support.AnswerSortingType;
import com.codestates.sof.domain.member.entity.Member;
import com.codestates.sof.domain.member.service.MemberService;
import com.codestates.sof.domain.question.entity.Question;
import com.codestates.sof.domain.question.service.QuestionService;
import com.codestates.sof.global.error.dto.ExceptionCode;
import com.codestates.sof.global.error.exception.BusinessLogicException;

@Transactional
@Service
public class AnswerService {
	private final AnswerRepository answerRepository;
	private final AnswerVoteRepository answerVoteRepository;
	private final MemberService memberService;
	private final QuestionService questionService;

	public AnswerService(AnswerRepository answerRepository, AnswerVoteRepository answerVoteRepository,
		MemberService memberService, QuestionService questionService) {
		this.answerRepository = answerRepository;
		this.answerVoteRepository = answerVoteRepository;
		this.memberService = memberService;
		this.questionService = questionService;
	}

	public Answer createAnswer(Answer answer) {
		Member findMember = memberService.findMember(answer.getMember().getMemberId());
		answer.setMember(findMember);

		Question findQuestion = questionService.findById(answer.getQuestion().getQuestionId());
		answer.setQuestion(findQuestion);

		Answer savedAnswer = answerRepository.save(answer);

		return savedAnswer;
	}

	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.SERIALIZABLE)
	public Answer updateAnswer(Answer answer, Member member) {
		Answer findAnswer = findVerifiedAnswer(answer.getAnswerId());
		verifyExistMember(findAnswer, member.getMemberId());

		Optional.ofNullable(answer.getContent())
			.ifPresent(findAnswer::setContent);

		findAnswer.setVoteCount(getAnswerVoteCount(answer));

		return answerRepository.save(findAnswer);
	}

	public Page<Answer> findAnswers(int page, int size, AnswerSortingType sort) {
		switch (sort) {
			case HIGH_SCORE:
				return answerRepository.findAll(PageRequest.of(page, size, Sort.by("voteCount").descending()));
			case NEWEST:
				return answerRepository.findAll(PageRequest.of(page, size, Sort.by("answerId").descending()));
			case OLDEST:
				return answerRepository.findAll(PageRequest.of(page, size, Sort.by("answerId").ascending()));
			default:
				throw new RuntimeException("Unexpected exception occurred.");
		}
	}

	public void deleteAnswer(long answerId, Member member) {
		Answer findAnswer = findVerifiedAnswer(answerId);
		verifyExistMember(findAnswer, member.getMemberId());

		answerRepository.delete(findAnswer);
	}

	@Transactional
	public Answer updateAnswerVote(AnswerVoteDto.Patch patch, Member member, long answerId) {
		Answer answer = findVerifiedAnswer(answerId);

		Optional<AnswerVote> optionalVote = answerVoteRepository.findByMemberAndAnswer(member, answer);

		AnswerVote vote = optionalVote.orElseGet(
			() -> answerVoteRepository.save(new AnswerVote(answer, member, patch.getVoteType())));

		vote.modify(patch.getVoteType());
		answer.setVoteCount(getAnswerVoteCount(answer));

		return answerRepository.save(answer);
	}

	public int getAnswerVoteCount(Answer answer) {
		return answer.getVotes()
			.stream()
			.mapToInt(vote -> vote.getType().getValue())
			.sum();
	}

	@Transactional(readOnly = true)
	public Answer findVerifiedAnswer(long answerId) {
		Optional<Answer> optionalAnswer = answerRepository.findById(answerId);

		Answer findAnswer =
			optionalAnswer.orElseThrow(() -> new BusinessLogicException(ExceptionCode.NOT_FOUND_ANSWER));

		return findAnswer;
	}

	@Transactional
	public void accept(Member member, Long questionId, long answerId) {
		Question question = questionService.findByIdWithoutIncreasingViewCount(questionId);

		if (!question.isEdditable(member))
			throw new BusinessLogicException(ExceptionCode.NO_PERMISSION_EDITING_QUESTION);

		Answer answer = findVerifiedAnswer(answerId);

		if (!answer.isGroupOf(question))
			throw new BusinessLogicException(ExceptionCode.NOT_FOUND_QUESTION);

		answer.setAccepted(true);
		question.acceptAnswer();
	}

	private void verifyExistMember(Answer answer, long memberId) {
		long memberIdByAnswer = answer.getMember().getMemberId();

		if (memberIdByAnswer == memberId) {
			return;
		}

		throw new BusinessLogicException(ExceptionCode.NO_PERMISSION_EDITING_ANSWER);
	}
}
