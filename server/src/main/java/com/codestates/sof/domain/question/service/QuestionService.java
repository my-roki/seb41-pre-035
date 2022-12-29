package com.codestates.sof.domain.question.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codestates.sof.domain.member.entity.Member;
import com.codestates.sof.domain.question.entity.Question;
import com.codestates.sof.domain.question.repository.QuestionRepository;
import com.codestates.sof.domain.question.support.QuestionPageRequest;
import com.codestates.sof.domain.question.support.QuestionSortingType;
import com.codestates.sof.domain.tag.entity.Tag;
import com.codestates.sof.domain.tag.service.TagService;
import com.codestates.sof.global.config.support.CustomPageRequest;
import com.codestates.sof.global.error.dto.ExceptionCode;
import com.codestates.sof.global.error.exception.BusinessLogicException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionService {
	private final QuestionRepository questionRepository;
	private final TagService tagService;

	@Transactional
	public Question write(Question question, Member member) {
		List<Tag> tags = tagService.findAllBy(question.getTagNames());
		question = new Question(member, question.getTitle(), question.getContent(), tags);
		question = save(question);
		question.increaseTaggedCountForAllTags();
		return question;
	}

	public Page<Question> findAll(CustomPageRequest<QuestionSortingType> pageRequest) {
		switch (pageRequest.getSortType()) {
			case NEWEST:
				return questionRepository.findAll(pageRequest.of());
			case UNADOPTED:
				return questionRepository.findAllByHasAdoptedAnswerIsFalse(pageRequest.of());
			case UNANSWERED:
				return questionRepository.findAllByAnswersEmpty(pageRequest.of());
			default:
				throw new RuntimeException("Unexpected exception occurred.");
		}
	}

	public Page<Question> findAllByTag(String tagName, QuestionPageRequest pageRequest) {
		Tag tag = tagService.findBy(tagName);
		switch (pageRequest.getSortType()) {
			case NEWEST:
				return questionRepository.findAllByTag(tag, pageRequest.of());
			case UNADOPTED:
				return questionRepository.findAllByTagAndHasAdoptedAnswerIsFalse(tag, pageRequest.of());
			case UNANSWERED:
				return questionRepository.findAllByTagAndAnswersEmpty(tag, pageRequest.of());
			default:
				throw new RuntimeException("Unexpected exception occurred.");
		}
	}

	public Question findByIdWithoutIncreasingViewCount(Long questionId) {
		return findExistsQuestion(questionId);
	}

	@Transactional
	public Question findById(Long questionId) {
		Question question = findExistsQuestion(questionId);
		question.increaseViewCount();
		return question;
	}

	@Transactional
	public Question patch(Long questionId, Member member, Question newQuestion) {
		Question question = findExistsQuestion(questionId);

		if (!question.isWrittenBy(member)) {
			throw new BusinessLogicException(ExceptionCode.NO_PERMISSION_EDITING_QUESTION);
		}

		List<Tag> tags = tagService.findAllBy(newQuestion.getTagNames());

		question.update(newQuestion, tags);

		return question;
	}

	public void delete(Member member, Long questionId) {
		Question question = findExistsQuestion(questionId);

		if (!question.isWrittenBy(member))
			throw new BusinessLogicException(ExceptionCode.NO_PERMISSION_EDITING_QUESTION);

		questionRepository.delete(question);
	}

	private Question findExistsQuestion(Long questionId) {
		return questionRepository.findByQuestionId(questionId)
			.orElseThrow(() -> new BusinessLogicException(ExceptionCode.NOT_FOUND_QUESTION));
	}

	private Question save(Question question) {
		return questionRepository.save(question);
	}
}
