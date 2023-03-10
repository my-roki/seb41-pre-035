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

import com.codestates.sof.domain.member.entity.Member;
import com.codestates.sof.domain.question.dto.QuestionCommentRequestDto;
import com.codestates.sof.domain.question.dto.QuestionCommentResponseDto;
import com.codestates.sof.domain.question.entity.QuestionComment;
import com.codestates.sof.domain.question.mapper.QuestionCommentMapper;
import com.codestates.sof.domain.question.service.QuestionCommentService;
import com.codestates.sof.domain.stub.QuestionCommentStub;
import com.fasterxml.jackson.databind.ObjectMapper;

@AutoConfigureRestDocs
@ActiveProfiles("local")
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
@WebMvcTest(controllers = QuestionCommentContoller.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class QuestionCommentContollerRestDocsTest {
	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper om;

	@MockBean
	QuestionCommentMapper mapper;

	@MockBean
	QuestionCommentService service;

	QuestionCommentRequestDto.Post post;
	QuestionCommentRequestDto.Patch patch;
	QuestionCommentResponseDto.Response response;

	@BeforeEach
	void beforeEach() {
		post = QuestionCommentStub.getDefaultPost();
		patch = QuestionCommentStub.getDefaultPatch();
		response = QuestionCommentStub.getDefaultResponse(false);
	}

	@Test
	void testForPost() throws Exception {
		// given
		QuestionComment temporalEntity = new QuestionComment(null, null, null);
		given(service.comment(any(Member.class), anyLong(), anyString())).willReturn(temporalEntity);
		given(mapper.commentToResponse(any())).willReturn(response);

		// when
		ResultActions actions = mvc.perform(
			post("/questions/{question-id}/comments", 1L)
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content(om.writeValueAsString(post))
				.header("Authorization", "Required JWT access token")
		);

		// then
		actions
			.andDo(print())
			.andExpect(status().isCreated())
			.andDo(
				getDefaultDocument(
					"question-comments/post",
					requestHeaders(headerWithName("Authorization").description("Jwt Access Token")),
					pathParameters(parameterWithName("question-id").description("?????? ?????????")),
					requestFields(fieldWithPath("content").type(JsonFieldType.STRING).description("?????? ??????")),
					getSingleResponseSnippet()
				)
			);
	}

	@Test
	void testForPatch() throws Exception {
		// given
		QuestionComment temporalEntity = new QuestionComment(null, null, null);
		given(service.modify(any(Member.class), anyLong(), anyLong(), anyString())).willReturn(temporalEntity);
		given(mapper.commentToResponse(any())).willReturn(response);

		// when
		ResultActions actions = mvc.perform(
			patch("/questions/{question-id}/comments/{comment-id}", 1L, 1L)
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
					"question-comments/patch",
					pathParameters(
						parameterWithName("question-id").description("?????? ?????????"),
						parameterWithName("comment-id").description("?????? ?????????")
					),
					requestHeaders(headerWithName("Authorization").description("Jwt Access Token")),
					requestFields(fieldWithPath("content").type(JsonFieldType.STRING).description("?????? ??????")),
					getSingleResponseSnippet()
				)
			);
	}

	@Test
	void testForGetAll() throws Exception {
		// given
		QuestionComment temporalEntity = new QuestionComment(null, null, null);
		Page<QuestionComment> page = new PageImpl<>(List.of(temporalEntity, temporalEntity), PageRequest.of(1, 10), 10);
		given(service.findAll(anyLong(), anyInt(), anyInt())).willReturn(page);
		given(mapper.commentToSimpleResponse(any(QuestionComment.class))).willReturn(
			QuestionCommentStub.getSimpleResponse());

		// when
		ResultActions actions = mvc.perform(
			get("/questions/{question-id}/comments", 1L)
				.param("page", "1")
				.param("size", "5")
		);

		// then
		actions
			.andExpect(status().isOk())
			.andDo(
				getDefaultDocument(
					"question-comments/get-all",
					pathParameters(parameterWithName("question-id").description("????????? ?????????")),
					requestParameters(
						parameterWithName("page").description("????????? ??????").optional(),
						parameterWithName("size").description("??????").optional()
					),
					getMultiResponseSnippet()
				)
			);
	}

	@Test
	void testForDelete() throws Exception {
		// given
		willDoNothing().given(service).delete(any(), anyLong(), anyLong());

		// when
		ResultActions actions = mvc.perform(
			delete("/questions/{question-id}/comments/{comment-id}", 1L, 1L)
				.header("Authorization", "Required JWT access token")
		);

		// then
		actions
			.andExpect(status().isNoContent())
			.andDo(
				getDefaultDocument(
					"question-comments/delete",
					requestHeaders(headerWithName("Authorization").description("Jwt Access Token")),
					pathParameters(
						parameterWithName("question-id").description("?????? ?????????"),
						parameterWithName("comment-id").description("?????? ?????????")
					)
				)
			);
	}

	private ResponseFieldsSnippet getSingleResponseSnippet() {
		return responseFields(
			List.of(
				fieldWithPath("data.member").type(JsonFieldType.OBJECT).description("?????? ???????????? ??????"),
				fieldWithPath("data.member.memberId").type(JsonFieldType.NUMBER).description("???????????? ?????????"),
				fieldWithPath("data.member.email").type(JsonFieldType.STRING).description("????????? ?????????"),
				fieldWithPath("data.member.name").type(JsonFieldType.STRING).description("????????? ??????"),
				fieldWithPath("data.member.verificationFlag").type(JsonFieldType.BOOLEAN).description("????????????"),
				fieldWithPath("data.member.deleteFlag").type(JsonFieldType.BOOLEAN).description("????????????"),
				fieldWithPath("data.member.lastActivateAt").type(JsonFieldType.STRING).description("????????? ????????????"),
				fieldWithPath("data.questionId").type(JsonFieldType.NUMBER).description("????????? ?????????"),
				fieldWithPath("data.content").type(JsonFieldType.STRING).description("?????? ??????"),
				fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("?????? ????????????"),
				fieldWithPath("data.lastModifiedAt").type(JsonFieldType.STRING).description("????????? ?????? ????????????")
			)
		);
	}

	private ResponseFieldsSnippet getMultiResponseSnippet() {
		return responseFields(
			fieldWithPath("data").type(JsonFieldType.ARRAY).description("?????? ??????"),
			fieldWithPath("pageInfo").type(JsonFieldType.OBJECT).description("????????? ??????")
		).andWithPrefix("data.[].",
			fieldWithPath("questionId").type(JsonFieldType.NUMBER).description("????????? ?????????"),
			fieldWithPath("memberId").type(JsonFieldType.NUMBER).description("???????????? ?????????"),
			fieldWithPath("memberName").type(JsonFieldType.STRING).description("???????????? ??????"),
			fieldWithPath("content").type(JsonFieldType.STRING).description("?????? ??????"),
			fieldWithPath("createdAt").type(JsonFieldType.STRING).description("????????????"),
			fieldWithPath("lastModifiedAt").type(JsonFieldType.STRING).description("????????? ????????????")
		).andWithPrefix("pageInfo.",
			fieldWithPath("page").type(JsonFieldType.NUMBER).description("????????? ?????????"),
			fieldWithPath("size").type(JsonFieldType.NUMBER).description("????????? ??????"),
			fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("??? ????????? ???"),
			fieldWithPath("totalElements").type(JsonFieldType.NUMBER).description("??? ??????")
		);
	}
}