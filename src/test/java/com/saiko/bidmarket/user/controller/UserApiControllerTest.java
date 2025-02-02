package com.saiko.bidmarket.user.controller;

import static com.saiko.bidmarket.product.Category.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saiko.bidmarket.common.exception.NotFoundException;
import com.saiko.bidmarket.common.Sort;
import com.saiko.bidmarket.product.entity.Product;
import com.saiko.bidmarket.user.controller.dto.UserBiddingSelectRequest;
import com.saiko.bidmarket.user.controller.dto.UserBiddingSelectResponse;
import com.saiko.bidmarket.user.controller.dto.UserHeartCheckResponse;
import com.saiko.bidmarket.user.controller.dto.UserHeartSelectRequest;
import com.saiko.bidmarket.user.controller.dto.UserHeartSelectResponse;
import com.saiko.bidmarket.user.controller.dto.UserProductSelectRequest;
import com.saiko.bidmarket.user.controller.dto.UserProductSelectResponse;
import com.saiko.bidmarket.user.controller.dto.UserSelectResponse;
import com.saiko.bidmarket.user.controller.dto.UserUpdateRequest;
import com.saiko.bidmarket.user.entity.Group;
import com.saiko.bidmarket.user.entity.User;
import com.saiko.bidmarket.user.service.UserService;
import com.saiko.bidmarket.util.ControllerSetUp;
import com.saiko.bidmarket.util.WithMockCustomLoginUser;

@WebMvcTest(controllers = UserApiController.class)
class UserApiControllerTest extends ControllerSetUp {

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private UserService userService;

  public static final String BASE_URL = "/api/v1/users";

  @Nested
  @DisplayName("updateUser 메서드는")
  @WithMockCustomLoginUser
  class DescribeUpdateUser {

    @Nested
    @DisplayName("유저 닉네임, 이미지의 수정 정보를 받으면")
    class ContextValidNickNameAndImage {

      @Test
      @DisplayName("Service의 updateUser메서드를 호출하고 OK를 반환한다.")
      void itReturnOkAndCallServiceMethod() throws Exception {
        //given
        final UserUpdateRequest requestDto = new UserUpdateRequest("test", "test");
        final String requestBody = objectMapper.writeValueAsString(requestDto);

        //when
        final MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .patch(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody);

        ResultActions response = mockMvc.perform(request);

        //then
        verify(userService).updateUser(anyLong(), any(UserUpdateRequest.class));
        response
            .andExpect(status().isOk())
            .andDo(document("Update User",
                            preprocessRequest(prettyPrint()),
                            requestFields(
                                fieldWithPath("username").type(JsonFieldType.STRING)
                                                         .description("변경할 유저 이름"),
                                fieldWithPath("profileImage").type(JsonFieldType.STRING)
                                                                .description("변경할 유저 프로필 이미지")
                            )
            ));
      }
    }

    @Nested
    @DisplayName("비어있는 이미지, 유저 닉네임을 모두 인자로 받으면")
    class ContextBlankUserName {

      @ParameterizedTest
      @NullAndEmptySource
      @ValueSource(strings = {"", " ", "\n", "\t"})
      @DisplayName("400 BadRequest를 반환한다.")
      void itThrow400BadRequest(String nullAndEmpty) throws Exception {
        //given
        final UserUpdateRequest requestDto = new UserUpdateRequest(nullAndEmpty, nullAndEmpty);
        final String requestBody = objectMapper.writeValueAsString(requestDto);

        //when
        final MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .patch(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody);

        ResultActions response = mockMvc.perform(request);

        //then
        response
            .andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("유효한 이미지, 비어있는 유저 닉네임을 인자로 받으면")
    class ContextEmptyUsername {

      @ParameterizedTest
      @NullAndEmptySource
      @ValueSource(strings = {"", " ", "\n", "\t"})
      @DisplayName("400 BadRequest를 반환한다.")
      void itThrow400BadRequest(String empty) throws Exception {
        //given
        final String validImage = "imageURL";
        final UserUpdateRequest requestDto = new UserUpdateRequest(empty, validImage);
        final String requestBody = objectMapper.writeValueAsString(requestDto);

        //when
        final MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .patch(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody);

        ResultActions response = mockMvc.perform(request);

        //then
        response
            .andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("비어있는 이미지, 유효한 유저 닉네임을 인자로 받으면")
    class ContextEmptyImage {

      @ParameterizedTest
      @NullAndEmptySource
      @ValueSource(strings = {"", " ", "\n", "\t"})
      @DisplayName("400 BadRequest를 반환한다.")
      void itThrow400BadRequest(String empty) throws Exception {
        //given
        final String validUsername = "test";
        final UserUpdateRequest requestDto = new UserUpdateRequest(validUsername, empty);
        final String requestBody = objectMapper.writeValueAsString(requestDto);

        //when
        final MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .patch(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody);

        ResultActions response = mockMvc.perform(request);

        //then
        response
            .andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("20자가 넘는 닉네임을 인자로 받으면")
    class ContextOverLengthUserName {

      @Test
      @DisplayName("400 BadRequest를 반환한다.")
      void itThrow400BadRequest() throws Exception {
        //given
        final String overLengthUsername = "testtesttesttesttesttestname";
        final UserUpdateRequest requestDto = new UserUpdateRequest(overLengthUsername, null);
        final String requestBody = objectMapper.writeValueAsString(requestDto);

        //when
        final MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .patch(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody);

        ResultActions response = mockMvc.perform(request);

        //then
        response
            .andExpect(status().isBadRequest());
      }
    }
  }

  @Nested
  @DisplayName("getUser메서드는 ")
  class DescribeGetUser {

    @Nested
    @DisplayName("유저 상세 정보 조회 요청을 받으면")
    class ContextReturnUserDetails {

      @Test
      @DisplayName("해당 유저의 Id와 이름과 사진URL을 반환한다.")
      void itReturnUsernameAndProfileImageUrl() throws Exception {
        //given
        long userId = 1L;
        final User user = new User("test",
                                   "test",
                                   "test",
                                   "test",
                                   new Group());
        ReflectionTestUtils.setField(user, "id", userId);

        //when
        when(userService.findById(anyLong())).thenReturn(UserSelectResponse.from(user));
        final MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .get(BASE_URL + "/" + userId);

        ResultActions response = mockMvc.perform(request);

        //then
        verify(userService).findById(anyLong());
        response
            .andExpect(status().isOk())
            .andDo(document("Get User",
                            preprocessRequest(prettyPrint()),
                            responseFields(
                                fieldWithPath("id").type(JsonFieldType.NUMBER)
                                                   .description("유저 식별자"),
                                fieldWithPath("username").type(JsonFieldType.STRING)
                                                         .description("현재 유저 이름"),
                                fieldWithPath("profileImage").type(JsonFieldType.STRING)
                                                             .description("현재 유저 프로필 이미지")
                            )
            ));
      }
    }

    @Nested
    @DisplayName("encoded 아이디가 존재하지 않는 유저의 Id라면")
    class ContextNotExistUserId {

      @Test
      @DisplayName("404 NotFound를 반환한다.")
      void itReturnBadRequest() throws Exception {

        //when
        when(userService.findById(anyLong())).thenThrow(NotFoundException.class);
        final MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .get(BASE_URL + "/" + 1L);

        ResultActions response = mockMvc.perform(request);

        //then
        verify(userService).findById(anyLong());
        response
            .andExpect(status().isNotFound());
      }
    }
  }

  @Nested
  @DisplayName("getUserInfo메서드는")
  @WithMockCustomLoginUser
  class DescribeGetUserInfo {

    @Nested
    @DisplayName("로그인 된 사용자의 토큰이 유효하다면")
    class ContextValidUserToken {

      @Test
      @DisplayName("해당 유저의 인코딩된 ID를 반환한다.")
      void itReturnEncodedUserID() throws Exception {
        //given
        final User user = new User("test",
                                   "test",
                                   "test",
                                   "test",
                                   new Group());

        ReflectionTestUtils.setField(user, "id", 1L);

        //when
        when(userService.findById(anyLong())).thenReturn(UserSelectResponse.from(user));
        final MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .get(BASE_URL + "/auth");

        ResultActions response = mockMvc.perform(request);

        //then
        verify(userService).findById(anyLong());
        response
            .andExpect(status().isOk())
            .andDo(document("Get UserInfo",
                            preprocessRequest(prettyPrint()),
                            responseFields(
                                fieldWithPath("id").type(JsonFieldType.NUMBER)
                                                   .description("유저 식별자"),
                                fieldWithPath("username").type(JsonFieldType.STRING)
                                                         .description("현재 유저 이름"),
                                fieldWithPath("profileImage").type(JsonFieldType.STRING)
                                                             .description("현재 유저 프로필 이미지")
                            )
            ));
      }
    }

    @Nested
    @DisplayName("로그인 된 사용자의 토큰이 유효하지만 존재하지 않는 유저라면")
    class ContextNotExistValidUserToken {

      @Test
      @DisplayName("404 NotFound를 반환한다.")
      void itReturnEncodedUserID() throws Exception {
        //when
        when(userService.findById(anyLong())).thenThrow(NotFoundException.class);
        final MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .get(BASE_URL + "/auth");

        ResultActions response = mockMvc.perform(request);

        //then
        verify(userService).findById(anyLong());
        response
            .andExpect(status().isNotFound());
      }
    }
  }

  @Nested
  @DisplayName("getAllUserProduct 메서드는")
  class DescribeGetAllUserProduct {

    @Nested
    @DisplayName("유효한 값이 전달되면")
    class ContextWithValidData {

      @Test
      @DisplayName("상품을 조회하고 결과를 반환한다")
      void ItReturnProductList() throws Exception {
        //given
        long userId = 1L;
        Product product = Product.builder()
                                 .title("감자팜")
                                 .description("가격 선제")
                                 .category(FOOD)
                                 .images(List.of("image1"))
                                 .location("강원도")
                                 .minimumPrice(1000)
                                 .writer(new User("감자킹", "image", "google", "123", new Group()))
                                 .build();

        ReflectionTestUtils.setField(product, "id", userId);
        ReflectionTestUtils.setField(product, "createdAt", LocalDateTime.now());

        List<UserProductSelectResponse> responses = List.of(
            UserProductSelectResponse.from(product));

        given(userService.findAllUserProducts(anyLong(),
                                              any(UserProductSelectRequest.class))).willReturn(
            responses);

        //when
        MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .get(BASE_URL + "/" + userId + "/products")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .queryParam("offset", "1")
            .queryParam("limit", "1")
            .queryParam("sort", Sort.END_DATE_ASC.name());

        ResultActions response = mockMvc.perform(request);

        //then
        response.andExpect(status().isOk())
                .andDo(document("Select product", preprocessRequest(
                    prettyPrint()), preprocessResponse(prettyPrint()), requestParameters(
                    parameterWithName("offset").description("상품 조회 시작 번호"),
                    parameterWithName("limit").description("상품 조회 개수"),
                    parameterWithName("sort").description("상품 정렬 기준").optional()), responseFields(
                    fieldWithPath("[].id").type(JsonFieldType.NUMBER).description("상품 식별자"),
                    fieldWithPath("[].title").type(JsonFieldType.STRING).description("상품 제목"),
                    fieldWithPath("[].thumbnailImage").type(JsonFieldType.STRING)
                                                      .description("상품 썸네일 이미지"),
                    fieldWithPath("[].minimumPrice").type(JsonFieldType.NUMBER)
                                                    .description("최소주문금액"),
                    fieldWithPath("[].heartCount").type(JsonFieldType.NUMBER)
                                                    .description("찜 갯수"),
                    fieldWithPath("[].expireAt").type(JsonFieldType.STRING).description("비딩 종료 시간"),
                    fieldWithPath("[].createdAt").type(JsonFieldType.STRING).description("생성 시간"),
                    fieldWithPath("[].updatedAt").type(JsonFieldType.STRING)
                                                 .description("수정 시간").optional())));
      }
    }

    @Nested
    @DisplayName("offset 에 숫자 외에 다른 문자가 들어온다면")
    class ContextNotNumberOffset {

      @Test
      @DisplayName("BadRequest 로 응답한다.")
      void itResponseBadRequest() throws Exception {
        // given
        String offset = "NotNumber";

        // when
        ResultActions response = mockMvc.perform(
            RestDocumentationRequestBuilders.get(BASE_URL + "/products").param("offset", offset));

        // then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("offset 에 음수가 들어온다면")
    class ContextNegativeNumberOffset {

      @Test
      @DisplayName("BadRequest 로 응답한다.")
      void itResponseBadRequest() throws Exception {
        // given

        // when
        ResultActions response = mockMvc.perform(
            RestDocumentationRequestBuilders.get(BASE_URL + "/products").param("offset", "-1"));

        // then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("limit 에 숫자 외에 다른 문자가 들어온다면")
    class ContextNotNumberLimit {

      @Test
      @DisplayName("BadRequest 로 응답한다.")
      void itResponseBadRequest() throws Exception {
        // given
        String limit = "NotNumber";

        // when
        ResultActions response = mockMvc.perform(
            RestDocumentationRequestBuilders.get(BASE_URL + "/products")
                                            .param("offset", "1")
                                            .param("limit", limit)
        );

        // then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("limit 에 양수가 아닌 숫자가 들어오면")
    class ContextNegativeOrZeroNumberLimit {

      @ParameterizedTest
      @ValueSource(strings = {"0", "-1"})
      @DisplayName("BadRequest 로 응답한다.")
      void itResponseBadRequest(String limit) throws Exception {
        // when
        ResultActions response = mockMvc.perform(
            RestDocumentationRequestBuilders.get(BASE_URL + "/products")
                                            .param("offset", "1")
                                            .param("limit", limit)
        );
        // then
        response.andExpect(status().isBadRequest());
      }
    }
  }

  @WithMockCustomLoginUser
  @Nested
  @DisplayName("getAllUserBidding 메서드는")
  class DescribeGetAllUserBidding {

    @Nested
    @DisplayName("유효한 값이 전달되면")
    class ContextWithValidData {

      @Test
      @DisplayName("입찰한 상품을 조회하고 결과를 반환한다")
      void ItReturnProductList() throws Exception {
        //given
        User user = User.builder()
                        .username("제로")
                        .profileImage("image")
                        .provider("google")
                        .providerId("123")
                        .group(new Group())
                        .build();
        Product product = Product.builder()
                                 .title("감자팜")
                                 .description("가격 선제")
                                 .category(FOOD)
                                 .images(List.of("image1"))
                                 .location("강원도")
                                 .minimumPrice(1000)
                                 .writer(user)
                                 .build();
        ReflectionTestUtils.setField(product, "id", 1L);
        ReflectionTestUtils.setField(product, "createdAt", LocalDateTime.now());

        given(userService.findAllUserBiddings(anyLong(), any(UserBiddingSelectRequest.class)))
            .willReturn(List.of(UserBiddingSelectResponse.from(product)));

        //when
        MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .get(BASE_URL + "/biddings")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .queryParam("offset", "1")
            .queryParam("limit", "1")
            .queryParam("sort", Sort.END_DATE_ASC.name());

        ResultActions response = mockMvc.perform(request);

        //then
        verify(userService).findAllUserBiddings(anyLong(), any(UserBiddingSelectRequest.class));
        response.andExpect(status().isOk())
                .andDo(document("Select bidding", preprocessRequest(
                    prettyPrint()), preprocessResponse(prettyPrint()), requestParameters(
                    parameterWithName("offset").description("상품 조회 시작 번호"),
                    parameterWithName("limit").description("상품 조회 개수"),
                    parameterWithName("sort").description("상품 정렬 기준").optional()), responseFields(
                    fieldWithPath("[].id").type(JsonFieldType.NUMBER).description("상품 식별자"),
                    fieldWithPath("[].title").type(JsonFieldType.STRING).description("상품 제목"),
                    fieldWithPath("[].thumbnailImage").type(JsonFieldType.STRING)
                                                      .description("상품 썸네일 이미지"),
                    fieldWithPath("[].minimumPrice").type(JsonFieldType.NUMBER)
                                                    .description("최소주문금액"),
                    fieldWithPath("[].heartCount").type(JsonFieldType.NUMBER)
                                                    .description("찜 갯수"),
                    fieldWithPath("[].expireAt").type(JsonFieldType.STRING).description("비딩 종료 시간"),
                    fieldWithPath("[].createdAt").type(JsonFieldType.STRING).description("생성 시간"),
                    fieldWithPath("[].updatedAt").type(JsonFieldType.STRING)
                                                 .description("수정 시간").optional())));
      }
    }

    @Nested
    @DisplayName("offset 에 숫자 외에 다른 문자가 들어온다면")
    class ContextNotNumberOffset {

      @Test
      @DisplayName("BadRequest 로 응답한다.")
      void itResponseBadRequest() throws Exception {
        // given
        String offset = "NotNumber";

        // when
        ResultActions response = mockMvc.perform(
            RestDocumentationRequestBuilders.get(BASE_URL + "/biddings").param("offset", offset));

        // then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("offset 에 음수가 들어온다면")
    class ContextNegativeNumberOffset {

      @Test
      @DisplayName("BadRequest 로 응답한다.")
      void itResponseBadRequest() throws Exception {
        // given

        // when
        ResultActions response = mockMvc.perform(
            RestDocumentationRequestBuilders.get(BASE_URL + "/biddings").param("offset", "-1"));

        // then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("limit 에 숫자 외에 다른 문자가 들어온다면")
    class ContextNotNumberLimit {

      @Test
      @DisplayName("BadRequest 로 응답한다.")
      void itResponseBadRequest() throws Exception {
        // given
        String limit = "NotNumber";

        // when
        ResultActions response = mockMvc.perform(
            RestDocumentationRequestBuilders.get(BASE_URL + "/biddings")
                                            .param("offset", "1")
                                            .param("limit", limit)
        );

        // then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("limit 에 양수가 아닌 숫자가 들어오면")
    class ContextNegativeOrZeroNumberLimit {

      @ParameterizedTest
      @ValueSource(strings = {"0", "-1"})
      @DisplayName("BadRequest 로 응답한다.")
      void itResponseBadRequest(String limit) throws Exception {
        // when
        ResultActions response = mockMvc.perform(
            RestDocumentationRequestBuilders.get(BASE_URL + "/biddings")
                                            .param("offset", "1")
                                            .param("limit", limit)
        );
        // then
        response.andExpect(status().isBadRequest());
      }
    }
  }

  @Nested
  @DisplayName("deleteUser 메서드는")
  @WithMockCustomLoginUser
  class DescribeDeleteUser {

    @Nested
    @DisplayName("Delete요청을 받으면")
    class ContextRecieveDeleteRequest {

      @Test
      @DisplayName("UserService의 deleteUser 메서드를 호출한다.")
      void itCallServiceDeleteUser() throws Exception {
        //when
        MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders.delete(BASE_URL);

        ResultActions response = mockMvc.perform(request);

        //then
        verify(userService).deleteUser(anyLong());
        response.andExpect(status().isOk());
      }
    }
  }

  @Nested
  @DisplayName("toggleHeart 메소드는")
  @WithMockCustomLoginUser
  class DescribeCheckToggleHeart {

    @Nested
    @DisplayName("유효한 값이 전달되면")
    class ContextWithValidData {

      @Test
      @DisplayName("찜 상태를 변경한다")
      void ItToggleHeart() throws Exception {
        //given
        long productId = 1L;

        //when
        MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .put(BASE_URL + "/{productId}/hearts", productId)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResultActions response = mockMvc.perform(request);

        //then
        response
            .andExpect(status().isOk())
            .andDo(document(
                "Toggle heart",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(parameterWithName("productId").description("상품 아이디"))
            ));
      }
    }

    @Nested
    @DisplayName("productId 에 숫자 외에 다른 문자가 들어온다면")
    class ContextNotNumberProductId {

      @Test
      @DisplayName("BadRequest 로 응답한다.")
      void itResponseBadRequest() throws Exception {
        // given
        String productId = "NotNumber";

        // when
        ResultActions response = mockMvc.perform(RestDocumentationRequestBuilders.put(
            BASE_URL + "/{productId}/hearts", productId));

        // then
        response.andExpect(status().isBadRequest());
      }
    }
  }

  @WithMockCustomLoginUser
  @Nested
  @DisplayName("getAllUserHearts 메서드는")
  class DescribeGetAllUserHearts {

    @Nested
    @DisplayName("유효한 값이 전달되면")
    class ContextWithValidData {

      @Test
      @DisplayName("찜한 상품을 조회하고 결과를 반환한다")
      void ItReturnProductList() throws Exception {
        //given
        User user = User.builder()
                        .username("제로")
                        .profileImage("image")
                        .provider("google")
                        .providerId("123")
                        .group(new Group())
                        .build();
        Product product = Product.builder()
                                 .title("감자팜")
                                 .description("가격 선제")
                                 .category(FOOD)
                                 .images(List.of("image1"))
                                 .location("강원도")
                                 .minimumPrice(1000)
                                 .writer(user)
                                 .build();
        ReflectionTestUtils.setField(product, "id", 1L);
        ReflectionTestUtils.setField(product, "createdAt", LocalDateTime.now());

        given(userService.findAllUserHearts(anyLong(), any(UserHeartSelectRequest.class)))
            .willReturn(List.of(UserHeartSelectResponse.from(product)));

        //when
        MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .get(BASE_URL + "/hearts")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .queryParam("offset", "1")
            .queryParam("limit", "1")
            .queryParam("sort", Sort.END_DATE_ASC.name());

        ResultActions response = mockMvc.perform(request);

        //then
        verify(userService).findAllUserHearts(anyLong(), any(UserHeartSelectRequest.class));
        response.andExpect(status().isOk())
                .andDo(document("Select bidding", preprocessRequest(
                    prettyPrint()), preprocessResponse(prettyPrint()), requestParameters(
                    parameterWithName("offset").description("상품 조회 시작 번호"),
                    parameterWithName("limit").description("상품 조회 개수"),
                    parameterWithName("sort").description("상품 정렬 기준").optional()), responseFields(
                    fieldWithPath("[].id").type(JsonFieldType.NUMBER).description("상품 식별자"),
                    fieldWithPath("[].title").type(JsonFieldType.STRING).description("상품 제목"),
                    fieldWithPath("[].thumbnailImage").type(JsonFieldType.STRING)
                                                      .description("상품 썸네일 이미지"),
                    fieldWithPath("[].minimumPrice").type(JsonFieldType.NUMBER)
                                                    .description("최소주문금액"),
                    fieldWithPath("[].heartCount").type(JsonFieldType.NUMBER)
                                                    .description("찜갯수"),
                    fieldWithPath("[].expireAt").type(JsonFieldType.STRING).description("비딩 종료 시간"),
                    fieldWithPath("[].createdAt").type(JsonFieldType.STRING).description("생성 시간"),
                    fieldWithPath("[].updatedAt").type(JsonFieldType.STRING)
                                                 .description("수정 시간").optional())));
      }
    }

    @Nested
    @DisplayName("offset 에 숫자 외에 다른 문자가 들어온다면")
    class ContextNotNumberOffset {

      @Test
      @DisplayName("BadRequest 로 응답한다.")
      void itResponseBadRequest() throws Exception {
        // given
        String offset = "NotNumber";

        // when
        ResultActions response = mockMvc.perform(RestDocumentationRequestBuilders
                                                     .get(BASE_URL + "/hearts")
                                                     .param("offset", offset));

        // then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("offset 에 음수가 들어온다면")
    class ContextNegativeNumberOffset {

      @Test
      @DisplayName("BadRequest 로 응답한다.")
      void itResponseBadRequest() throws Exception {
        // given

        // when
        ResultActions response = mockMvc.perform(RestDocumentationRequestBuilders
                                                     .get(BASE_URL + "/hearts")
                                                     .param("offset", "-1"));

        // then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("limit 에 숫자 외에 다른 문자가 들어온다면")
    class ContextNotNumberLimit {

      @Test
      @DisplayName("BadRequest 로 응답한다.")
      void itResponseBadRequest() throws Exception {
        // given
        String limit = "NotNumber";

        // when
        ResultActions response = mockMvc.perform(RestDocumentationRequestBuilders
                                                     .get(BASE_URL + "/hearts")
                                                     .param("offset", "1")
                                                     .param("limit", limit));

        // then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("limit 에 양수가 아닌 숫자가 들어오면")
    class ContextNegativeOrZeroNumberLimit {

      @ParameterizedTest
      @ValueSource(strings = {"0", "-1"})
      @DisplayName("BadRequest 로 응답한다.")
      void itResponseBadRequest(String limit) throws Exception {
        // when
        ResultActions response = mockMvc.perform(RestDocumentationRequestBuilders
                                                     .get(BASE_URL + "/hearts")
                                                     .param("offset", "1")
                                                     .param("limit", limit));

        // then
        response.andExpect(status().isBadRequest());
      }
    }
  }

  @Nested
  @DisplayName("isUserHeart 메서드는")
  @WithMockCustomLoginUser
  class DescribeIsUserHeart {

    @Nested
    @DisplayName("유효한 값을 받아 호출되면")
    class ContextValidCall {

      @Test
      @DisplayName("200 ok와 서비스 isUaerHeart 메서드를 호출한다.")
      void itCallServiceIsUserHeart() throws Exception {
        //given
        final long productId = 1;
        final UserHeartCheckResponse responseDto = new UserHeartCheckResponse(true);

        when(userService.isUserHearts(anyLong(), anyLong())).thenReturn(responseDto);
        //when
        MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .get(BASE_URL + "/" + productId + "/hearts");

        ResultActions response = mockMvc.perform(request);

        //then
        verify(userService).isUserHearts(anyLong(), anyLong());
        response
            .andExpect(status().isOk())
            .andDo(
                document(
                    "isUserHeart",
                    preprocessResponse(prettyPrint()),
                    responseFields(
                        fieldWithPath("heart")
                            .type(JsonFieldType.BOOLEAN)
                            .description("유저 찜 여부")
                    )
                )
            );
      }
    }

    @Nested
    @DisplayName("존재하지 않는 상품 id를 받으면")
    class ContextNotExistProductId {

      @Test
      @DisplayName("NotFound Exception을 반환한다.")
      void itThrowNotFoundException() throws Exception {
        //given
        final long productId = 1;

        when(userService.isUserHearts(anyLong(), anyLong())).thenThrow(NotFoundException.class);
        //when
        MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .get(BASE_URL + "/" + productId + "/hearts");

        ResultActions response = mockMvc.perform(request);

        //then
        verify(userService).isUserHearts(anyLong(), anyLong());
        response
            .andExpect(status().isNotFound());
      }
    }
  }
}
