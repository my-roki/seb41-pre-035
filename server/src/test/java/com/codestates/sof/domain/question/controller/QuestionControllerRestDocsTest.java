package com.codestates.sof.domain.question.controller;

import static com.codestates.sof.global.utils.AsciiUtils.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.codestates.sof.domain.question.dto.QuestionRequestDto;
import com.codestates.sof.domain.question.dto.QuestionResponseDto;
import com.codestates.sof.domain.question.entity.Question;
import com.codestates.sof.domain.question.mapper.QuestionMapper;
import com.codestates.sof.domain.question.service.QuestionService;
import com.codestates.sof.domain.question.support.QuestionPageRequest;
import com.codestates.sof.domain.stub.QuestionStub;
import com.codestates.sof.global.utils.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

@AutoConfigureRestDocs
@ActiveProfiles("local")
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
@WebMvcTest(controllers = QuestionController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class QuestionControllerRestDocsTest {
	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper om;

	@MockBean
	QuestionMapper mapper;

	@MockBean
	QuestionService service;

	Question question;
	QuestionRequestDto.Post post;
	QuestionResponseDto.Response response;
	QuestionResponseDto.SimpleResponse simpleResponse;

	@BeforeEach
	void beforeEach() {
		post = QuestionStub.getPostRequest();
		question = QuestionStub.getDefaultQuestion();
		response = QuestionStub.getSingleResponse();
		simpleResponse = QuestionStub.getSimpleResponse();
	}

	@Test
	void testForWrite() throws Exception {
		// given
		given(service.write(any(), any())).willReturn(question);
		given(mapper.questionToResponse(any())).willReturn(response);
		given(mapper.postToQuestion(any())).willReturn(question);

		// when
		ResultActions actions = mvc.perform(
			TestUtils.POST.apply("/questions", om.writeValueAsString(post))
				.header("Authorization", "Required JWT access token")
		);

		// then
		actions
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.questionId").value(1))
			.andExpect(jsonPath("$.data.createdAt").exists())
			.andExpect(jsonPath("$.data.lastModifiedAt").exists())
			.andDo(getDefaultDocument(
					"question/post",
					requestHeaders(headerWithName("Authorization").description("Jwt Access Token")),
					requestFields(
						List.of(
							fieldWithPath("title").type(JsonFieldType.STRING).description("?????? ??????"),
							fieldWithPath("content").type(JsonFieldType.STRING).description("?????? ??????"),
							fieldWithPath("tags").type(JsonFieldType.ARRAY).description("????????? ????????? ???????????? ??????")
						)
					),
					getSingleResponseSnippet()
				)
			);
	}

	@Test
	void testForGet() throws Exception {
		// given
		given(service.findById(any(Long.class))).willReturn(question);
		given(mapper.questionToResponse(any(Question.class))).willReturn(response);

		// when
		ResultActions actions = mvc.perform(
			get("/questions/{question-id}", question.getQuestionId())
				.header("Authorization", "Required JWT access token")
		);

		// then
		actions
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.questionId").value(question.getQuestionId()))
			.andExpect(jsonPath("$.data.writer.memberId").value(question.getMember().getMemberId()))
			.andExpect(jsonPath("$.data.lastModifiedAt").exists())
			.andDo(
				getDefaultDocument(
					"question/get",
					requestHeaders(headerWithName("Authorization").description("Jwt Access Token")),
					pathParameters(parameterWithName("question-id").description("?????? ?????????")),
					getSingleResponseSnippet()
				)
			);
	}

	@Test
	void testForGetAll() throws Exception {
		// given
		Page<Question> page = new PageImpl<>(List.of(question), PageRequest.of(1, 10), 10);
		given(service.findAll(any(QuestionPageRequest.class))).willReturn(page);
		given(mapper.questionsToResponses(any(), any())).willReturn(List.of(simpleResponse));

		// when
		ResultActions actions = mvc.perform(
			get("/questions", question.getQuestionId())
				.param("page", "1")
				.param("size", "5")
				.param("sort", "NEWEST")
				.header("Authorization", "Required JWT access token")
		);

		// then
		actions
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.pageInfo").exists())
			.andDo(
				getDefaultDocument(
					"question/get-all",
					requestHeaders(headerWithName("Authorization").description("Jwt Access Token")),
					requestParameters(
						parameterWithName("page").description("????????? ??????").optional(),
						parameterWithName("size").description("??????").optional(),
						parameterWithName("sort").description("?????? ?????? (NEWEST | UNACCEPTED | UNANSWERED)").optional()
					),
					getMultiResponseSnippet()
				)
			);
	}

	@Test
	void testForGetAllByTags() throws Exception {
		// given
		Page<Question> page = new PageImpl<>(List.of(question), PageRequest.of(1, 10), 10);
		given(service.findAllByTag(anyString(), any(QuestionPageRequest.class))).willReturn(page);
		given(mapper.questionsToResponses(any(), any())).willReturn(List.of(simpleResponse));

		// when
		ResultActions actions = mvc.perform(
			get("/questions/tagged/{tag-name}", "java")
				.param("page", "1")
				.param("size", "5")
				.param("sort", "POPULAR")
				.header("Authorization", "Required JWT access token")
		);

		// then
		actions
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.pageInfo").exists())
			.andDo(
				getDefaultDocument(
					"question/get-all-by-tag",
					requestHeaders(headerWithName("Authorization").description("Jwt Access Token")),
					requestParameters(
						parameterWithName("page").description("????????? ??????").optional(),
						parameterWithName("size").description("??????").optional(),
						parameterWithName("sort").description("?????? ?????? (NEWEST | UNACCEPTED | UNANSWERED)").optional()
					),
					pathParameters(parameterWithName("tag-name").description("?????????")),
					getMultiResponseSnippet()
				)
			);
	}

	@Test
	void testForGetAllByQuery() throws Exception {
		// given
		Page<Question> page = new PageImpl<>(List.of(question), PageRequest.of(1, 10), 10);
		given(service.search(anyString(), any(QuestionPageRequest.class))).willReturn(page);
		given(mapper.questionsToResponses(any(), any())).willReturn(List.of(simpleResponse));

		// when
		ResultActions actions = mvc.perform(
			get("/questions/search")
				.param("q", "text")
				.param("page", "1")
				.param("size", "5")
				.param("sort", "POPULAR")
				.header("Authorization", "Required JWT access token")
		);

		// then
		actions
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.pageInfo").exists())
			.andDo(
				getDefaultDocument(
					"question/search",
					requestHeaders(headerWithName("Authorization").description("Jwt Access Token")),
					requestParameters(
						parameterWithName("q").description("????????? ??????"),
						parameterWithName("page").description("????????? ??????").optional(),
						parameterWithName("size").description("??????").optional(),
						parameterWithName("sort").description("?????? ?????? (NEWEST | UNACCEPTED | UNANSWERED)").optional()
					),
					getMultiResponseSnippet()
				)
			);
	}

	@Test
	void testForPatch() throws Exception {
		// given
		QuestionRequestDto.Patch patch = QuestionStub.getPatchRequest();

		given(service.patch(anyLong(), any(), any())).willReturn(question);
		given(mapper.questionToResponse(any())).willReturn(response);

		// when
		ResultActions actions = mvc.perform(
			patch("/questions/{question-id}", 1L)
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content(om.writeValueAsString(patch))
				.header("Authorization", "Required JWT access token")
		);

		// then
		actions
			.andExpect(status().isOk())
			.andDo(
				getDefaultDocument(
					"question/patch",
					requestHeaders(headerWithName("Authorization").description("Jwt Access Token")),
					pathParameters(parameterWithName("question-id").description("?????? ?????????")),
					requestFields(
						List.of(
							fieldWithPath("title").type(JsonFieldType.STRING).description("????????? ?????? ??????"),
							fieldWithPath("content").type(JsonFieldType.STRING).description("????????? ?????? ??????"),
							fieldWithPath("tags").type(JsonFieldType.ARRAY).description("????????? ?????????")
						)
					),
					getSingleResponseSnippet()
				)
			);
	}

	@Test
	void testForDelete() throws Exception {
		// given
		willDoNothing().given(service).delete(any(), any());

		// when
		ResultActions actions = mvc.perform(
			delete("/questions/{question-id}", 1L)
				.header("Authorization", "Required JWT access token")
		);

		// then
		actions
			.andExpect(status().isNoContent())
			.andDo(
				getDefaultDocument(
					"question/delete",
					requestHeaders(headerWithName("Authorization").description("Jwt Access Token")),
					pathParameters(parameterWithName("question-id").description("?????? ?????????"))
				)
			);
	}

	public static ResponseFieldsSnippet getSingleResponseSnippet() {
		return responseFields(
			fieldWithPath("data").type(JsonFieldType.OBJECT).description("?????? ?????????")
		).andWithPrefix("data.",
			fieldWithPath("questionId").type(JsonFieldType.NUMBER).description("?????? ?????????"),
			fieldWithPath("title").type(JsonFieldType.STRING).description("?????? ??????"),
			fieldWithPath("content").type(JsonFieldType.STRING).description("?????? ??????"),
			fieldWithPath("viewCount").type(JsonFieldType.NUMBER).description("?????????"),
			fieldWithPath("voteCount").type(JsonFieldType.NUMBER).description("??? ?????????"),
			fieldWithPath("voteType").type(JsonFieldType.STRING).description("???????????? [UP | DOWN | NONE]"),
			fieldWithPath("isItWriter").type(JsonFieldType.BOOLEAN).description("????????? ??????"),
			fieldWithPath("isBookmarked").type(JsonFieldType.BOOLEAN).description("????????? ??????"),
			fieldWithPath("createdAt").type(JsonFieldType.STRING).description("????????????"),
			fieldWithPath("lastModifiedAt").type(JsonFieldType.STRING).description("????????? ????????????"),
			fieldWithPath("writer").type(JsonFieldType.OBJECT).description("???????????? ??????"),
			fieldWithPath("tags").type(JsonFieldType.ARRAY).description("???????????? ??????"),
			fieldWithPath("answers").type(JsonFieldType.ARRAY).description("???????????? ??????")
		).andWithPrefix("data.writer.",
			fieldWithPath("memberId").type(JsonFieldType.NUMBER).description("???????????? ?????????"),
			fieldWithPath("email").type(JsonFieldType.STRING).description("????????? ?????????"),
			fieldWithPath("name").type(JsonFieldType.STRING).description("????????? ??????"),
			fieldWithPath("verificationFlag").type(JsonFieldType.BOOLEAN).description("????????????"),
			fieldWithPath("deleteFlag").type(JsonFieldType.BOOLEAN).description("????????????"),
			fieldWithPath("lastActivateAt").type(JsonFieldType.STRING).description("????????? ????????????")
		).andWithPrefix("data.tags.[].",
			fieldWithPath("tagId").type(JsonFieldType.NUMBER).description("????????? ?????????"),
			fieldWithPath("name").type(JsonFieldType.STRING).description("?????????")
		).andWithPrefix("data.answers.[].",
			fieldWithPath("answerId").type(JsonFieldType.NUMBER).description("???????????? ?????????"),
			fieldWithPath("questionId").type(JsonFieldType.NUMBER).description("????????? ????????? ????????? ?????????"),
			fieldWithPath("memberId").type(JsonFieldType.NUMBER).description("????????? ??????"),
			fieldWithPath("content").type(JsonFieldType.STRING).description("?????? ??????"),
			fieldWithPath("voteCount").type(JsonFieldType.NUMBER).description("??? ?????????"),
			fieldWithPath("isAccepted").type(JsonFieldType.BOOLEAN).description("??????????????????"),
			fieldWithPath("isItWriter").type(JsonFieldType.BOOLEAN).description("???????????????"),
			fieldWithPath("hasAlreadyVoted").type(JsonFieldType.BOOLEAN).description("????????????"),
			fieldWithPath("createdAt").type(JsonFieldType.STRING).description("????????????"),
			fieldWithPath("lastModifiedAt").type(JsonFieldType.STRING).description("????????? ????????????")
		);
	}

	public static ResponseFieldsSnippet getMultiResponseSnippet() {
		return responseFields(
			fieldWithPath("data").type(JsonFieldType.ARRAY).description("?????? ??????"),
			fieldWithPath("pageInfo").type(JsonFieldType.OBJECT).description("????????? ??????")
		).andWithPrefix("data.[].",
			fieldWithPath("questionId").type(JsonFieldType.NUMBER).description("????????? ?????????"),
			fieldWithPath("writerId").type(JsonFieldType.NUMBER).description("???????????? ?????????"),
			fieldWithPath("writerName").type(JsonFieldType.STRING).description("???????????? ??????"),
			fieldWithPath("title").type(JsonFieldType.STRING).description("?????? ??????"),
			fieldWithPath("content").type(JsonFieldType.STRING).description("?????? ??????"),
			fieldWithPath("answerCount").type(JsonFieldType.NUMBER).description("?????? ???"),
			fieldWithPath("viewCount").type(JsonFieldType.NUMBER).description("?????????"),
			fieldWithPath("voteCount").type(JsonFieldType.NUMBER).description("??? ?????????"),
			fieldWithPath("hasAlreadyVoted").type(JsonFieldType.BOOLEAN).description("????????????"),
			fieldWithPath("hasAcceptedAnswer").type(JsonFieldType.BOOLEAN).description("?????? ????????????"),
			fieldWithPath("createdAt").type(JsonFieldType.STRING).description("????????????"),
			fieldWithPath("lastModifiedAt").type(JsonFieldType.STRING).description("????????? ????????????"),
			fieldWithPath("tags").type(JsonFieldType.ARRAY).description("???????????? ??????")
		).andWithPrefix("data.[].tags.[].",
			fieldWithPath("tagId").type(JsonFieldType.NUMBER).description("????????? ?????????"),
			fieldWithPath("name").type(JsonFieldType.STRING).description("?????????")
		).andWithPrefix("pageInfo.",
			fieldWithPath("page").type(JsonFieldType.NUMBER).description("????????? ?????????"),
			fieldWithPath("size").type(JsonFieldType.NUMBER).description("????????? ??????"),
			fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("??? ????????? ???"),
			fieldWithPath("totalElements").type(JsonFieldType.NUMBER).description("??? ??????")
		);
	}
}