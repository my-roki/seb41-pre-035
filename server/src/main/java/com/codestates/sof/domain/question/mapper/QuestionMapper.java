package com.codestates.sof.domain.question.mapper;

import java.util.List;
import java.util.Optional;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import com.codestates.sof.domain.answer.mapper.AnswerMapper;
import com.codestates.sof.domain.common.VoteType;
import com.codestates.sof.domain.member.entity.Member;
import com.codestates.sof.domain.member.mapper.MemberMapper;
import com.codestates.sof.domain.question.dto.QuestionRequestDto;
import com.codestates.sof.domain.question.dto.QuestionResponseDto;
import com.codestates.sof.domain.question.entity.Question;
import com.codestates.sof.domain.tag.mapper.TagMapper;

@Mapper(componentModel = "spring", uses = {MemberMapper.class, TagMapper.class, AnswerMapper.class})
public interface QuestionMapper {
	@Mapping(target = "writer", source = "member")
	@Mapping(target = "voteCount", expression = "java(question.getVoteCount())")
	QuestionResponseDto.Response questionToResponse(Question question);

	@Mapping(target = "writerId", source = "member.memberId")
	@Mapping(target = "writerName", source = "member.name")
	@Mapping(target = "voteCount", expression = "java(question.getVoteCount())")
	@Mapping(target = "answerCount", expression = "java(question.getAnswers().size())")
	QuestionResponseDto.SimpleResponse questionToSimpleResponse(Question question);

	default Question postToQuestion(QuestionRequestDto.Post post) {
		Question question = new Question(null, post.getTitle(), post.getContent());
		Optional.ofNullable(post.getTags())
			.ifPresent(tags -> post.getTags().forEach(question::addTag));
		return question;
	}

	default Question patchToQuestion(QuestionRequestDto.Patch patch) {
		Question question = new Question(null, patch.getTitle(), patch.getContent());
		Optional.ofNullable(patch.getTags())
			.ifPresent(tags -> patch.getTags().forEach(question::addTag));
		return question;
	}

	default List<QuestionResponseDto.SimpleResponse> questionsToResponses(Page<Question> questions, Member member) {
		return questions.map(question -> {
			QuestionResponseDto.SimpleResponse response = questionToSimpleResponse(question);
			Optional.ofNullable(member)
				.ifPresent(presentMember -> response.setHasAlreadyVoted(question.getVoteType(presentMember) != VoteType.NONE));
			return response;
		}).toList();
	}

	default void setPropertiesToResponse(Member member, Question question, QuestionResponseDto.Response response) {
		Optional.ofNullable(member)
				.ifPresent(presentMember -> {
					response.setIsItWriter(question.isWrittenBy(presentMember));
					response.setVoteType(question.getVoteType(presentMember));
					response.setBookmarked(question.isBookmarked(presentMember));
				});
	}
}
