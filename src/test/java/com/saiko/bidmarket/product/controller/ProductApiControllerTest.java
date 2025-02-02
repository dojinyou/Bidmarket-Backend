package com.saiko.bidmarket.product.controller;

import static com.saiko.bidmarket.product.Category.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.anyLong;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
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
import com.saiko.bidmarket.common.Sort;
import com.saiko.bidmarket.product.controller.dto.BiddingResultResponse;
import com.saiko.bidmarket.product.controller.dto.ProductCreateRequest;
import com.saiko.bidmarket.product.controller.dto.ProductCreateResponse;
import com.saiko.bidmarket.product.controller.dto.ProductDetailResponse;
import com.saiko.bidmarket.product.controller.dto.ProductSelectRequest;
import com.saiko.bidmarket.product.controller.dto.ProductSelectResponse;
import com.saiko.bidmarket.product.entity.Image;
import com.saiko.bidmarket.product.entity.Product;
import com.saiko.bidmarket.product.service.ProductService;
import com.saiko.bidmarket.user.entity.Group;
import com.saiko.bidmarket.user.entity.User;
import com.saiko.bidmarket.util.ControllerSetUp;
import com.saiko.bidmarket.util.WithMockCustomLoginUser;

@WebMvcTest(controllers = ProductApiController.class)
class ProductApiControllerTest extends ControllerSetUp {
  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ProductService productService;

  public static final String BASE_URL = "/api/v1/products";

  static class TitleSourceOutOfRange implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          Arguments.of((Object)null),
          Arguments.of(""),
          Arguments.of("\t"),
          Arguments.of("\n"),
          Arguments.of("a".repeat(33))
      );
    }
  }

  static class DescriptionSourceOutOfRange implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          Arguments.of((Object)null),
          Arguments.of(""),
          Arguments.of("\t"),
          Arguments.of("\n"),
          Arguments.of("a".repeat(501))
      );
    }
  }

  static class LocationSourceOutOfRange implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          Arguments.of("a".repeat(21))
      );
    }
  }

  private static Product product = Product
      .builder()
      .title("귤 팔아요")
      .description("맛있어요")
      .category(FOOD)
      .images(List.of("image1"))
      .location("제주도")
      .minimumPrice(1000)
      .writer(new User("제로", "image", "google", "123", new Group()))
      .build();
  private static long productId = 1;

  @BeforeAll
  static void setup() {
    ReflectionTestUtils.setField(product, "id", productId);
    ReflectionTestUtils.setField(product, "createdAt", LocalDateTime.now());

  }

  @Nested
  @DisplayName("create 메서드는")
  @WithMockCustomLoginUser
  class DescribeCreate {

    @Nested
    @DisplayName("유효한 값이 전달되면")
    class ContextWithValidData {
      @Test
      @DisplayName("상품을 저장하고 상품의 id 값을 반환한다")
      void ItSaveProductAndReturnId() throws Exception {
        //given
        HashMap<String, Object> requestMap = new HashMap<>();
        requestMap.put("title", "키보드팝니다");
        requestMap.put("description", "깨끗합니다");
        requestMap.put("category", DIGITAL_DEVICE.name());
        requestMap.put("minimumPrice", 10000);
        requestMap.put("location", "관악구 신림동");
        requestMap.put("images", new String[]{"imageUrl1, imageUrl2"});

        String requestBody = objectMapper.writeValueAsString(requestMap);

        given(productService.create(any(ProductCreateRequest.class), any(Long.class)))
            .willReturn(ProductCreateResponse.from(1L));

        //when
        MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody);

        ResultActions response = mockMvc.perform(request);

        //then
        verify(productService).create(any(ProductCreateRequest.class), any(Long.class));
        response
            .andExpect(status().isCreated())
            .andDo(document(
                "Create product",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(
                    fieldWithPath("title")
                        .type(JsonFieldType.STRING)
                        .description("제목"),
                    fieldWithPath("description")
                        .type(JsonFieldType.STRING)
                        .description("설명"),
                    fieldWithPath("images")
                        .type(JsonFieldType.ARRAY)
                        .description("상품 이미지")
                        .optional(),
                    fieldWithPath("category")
                        .type(JsonFieldType.STRING)
                        .description("카테고리"),
                    fieldWithPath("minimumPrice")
                        .type(JsonFieldType.NUMBER)
                        .description("최소 가격"),
                    fieldWithPath("location")
                        .type(JsonFieldType.STRING)
                        .description("희망 거래 장소")
                        .optional()
                ),
                responseFields(
                    fieldWithPath("id")
                        .type(JsonFieldType.NUMBER)
                        .description("상품 아이디")
                )
            ));
      }
    }

    @Nested
    @DisplayName("title 의 길이가 범위를 벗어나면")
    class ContextWithTitleOutOfRange {

      @ParameterizedTest
      @ArgumentsSource(TitleSourceOutOfRange.class)
      @DisplayName("BadRequest 를 응답한다")
      void ItResponseBadRequest(String title) throws Exception {
        //given
        HashMap<String, Object> requestMap = new HashMap<>();
        requestMap.put("title", title);
        requestMap.put("description", "깨끗합니다");
        requestMap.put("category", DIGITAL_DEVICE);
        requestMap.put("minimumPrice", 10000);
        requestMap.put("location", "관악구 신림동");
        requestMap.put("images", new String[]{"imageUrl1, imageUrl2"});

        String requestBody = objectMapper.writeValueAsString(requestMap);

        //when
        MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody);

        ResultActions response = mockMvc.perform(request);

        //then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("description 의 길이가 범위를 벗어나면")
    class ContextWithDescriptionOutOfRange {

      @ParameterizedTest
      @ArgumentsSource(DescriptionSourceOutOfRange.class)
      @DisplayName("BadRequest 를 응답한다")
      void ItResponseBadRequest(String description) throws Exception {
        //given
        HashMap<String, Object> requestMap = new HashMap<>();
        requestMap.put("title", "키보드팝니다");
        requestMap.put("description", description);
        requestMap.put("category", DIGITAL_DEVICE);
        requestMap.put("minimumPrice", 10000);
        requestMap.put("location", "관악구 신림동");
        requestMap.put("imageUrl1", new String[]{"imageUrl1, imageUrl2"});

        String requestBody = objectMapper.writeValueAsString(requestMap);

        //when
        MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody);

        ResultActions response = mockMvc.perform(request);

        //then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("location 의 길이가 범위를 벗어나면")
    class ContextWithLocationOutOfRange {

      @ParameterizedTest
      @ArgumentsSource(LocationSourceOutOfRange.class)
      @DisplayName("BadRequest 를 응답한다")
      void ItResponseBadRequest(String location) throws Exception {
        //given
        HashMap<String, Object> requestMap = new HashMap<>();
        requestMap.put("title", "키보드팝니다");
        requestMap.put("description", "빨리 사세요");
        requestMap.put("category", DIGITAL_DEVICE);
        requestMap.put("minimumPrice", 10000);
        requestMap.put("location", location);
        requestMap.put("imageUrl1", new String[]{"imageUrl1, imageUrl2"});

        String requestBody = objectMapper.writeValueAsString(requestMap);

        //when
        MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody);

        ResultActions response = mockMvc.perform(request);

        //then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("images 의 길이가 범위를 벗어나면")
    class ContextWithImagesOutOfRange {

      @Test
      @DisplayName("BadRequest 를 응답한다")
      void ItResponseBadRequest() throws Exception {
        //given
        HashMap<String, Object> requestMap = new HashMap<>();
        requestMap.put("title", "키보드팝니다");
        requestMap.put("description", "깨끗합니다");
        requestMap.put("category", DIGITAL_DEVICE);
        requestMap.put("minimumPrice", 10000);
        requestMap.put("location", "관악구 신림동");
        requestMap.put("images", Arrays.asList(
            "imageUrl1",
            "imageUrl2",
            "imageUrl3",
            "imageUrl4",
            "imageUrl5",
            "imageUrl6"
        ));

        String requestBody = objectMapper.writeValueAsString(requestMap);

        //when
        MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody);

        ResultActions response = mockMvc.perform(request);

        //then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("images 가 null 이라면")
    class ContextWithImagesNull {

      @Test
      @DisplayName("BadRequest 를 응답한다")
      void ItResponseBadRequest() throws Exception {
        //given
        HashMap<String, Object> requestMap = new HashMap<>();
        requestMap.put("title", "키보드팝니다");
        requestMap.put("description", "깨끗합니다");
        requestMap.put("category", DIGITAL_DEVICE);
        requestMap.put("minimumPrice", 10000);
        requestMap.put("location", "관악구 신림동");
        requestMap.put("images", null);

        String requestBody = objectMapper.writeValueAsString(requestMap);

        //when
        MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody);

        ResultActions response = mockMvc.perform(request);

        //then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("images 가 빈 값 이라면")
    class ContextWithImagesEmpty {

      @Test
      @DisplayName("BadRequest 를 응답한다")
      void ItResponseBadRequest() throws Exception {
        //given
        HashMap<String, Object> requestMap = new HashMap<>();
        requestMap.put("title", "키보드팝니다");
        requestMap.put("description", "깨끗합니다");
        requestMap.put("category", DIGITAL_DEVICE);
        requestMap.put("minimumPrice", 10000);
        requestMap.put("location", "관악구 신림동");
        requestMap.put("images", Collections.EMPTY_LIST);

        String requestBody = objectMapper.writeValueAsString(requestMap);

        //when
        MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody);

        ResultActions response = mockMvc.perform(request);

        //then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("minimumPrice 가 범위를 벗어나면")
    class ContextWithMinimumPriceOutOfRange {

      @Test
      @DisplayName("BadRequest 를 응답한다")
      void ItResponseBadRequest() throws Exception {
        //given
        HashMap<String, Object> requestMap = new HashMap<>();
        requestMap.put("title", "키보드팝니다");
        requestMap.put("description", "깨끗합니다");
        requestMap.put("category", DIGITAL_DEVICE);
        requestMap.put("minimumPrice", 100);
        requestMap.put("location", "관악구 신림동");
        requestMap.put("imageUrl1", new String[]{"imageUrl1, imageUrl2"});

        String requestBody = objectMapper.writeValueAsString(requestMap);

        //when
        MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody);

        ResultActions response = mockMvc.perform(request);

        //then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("category 가 null 이라면")
    class ContextWithCategoryNull {

      @Test
      @DisplayName("BadRequest 를 응답한다")
      void ItResponseBadRequest() throws Exception {
        //given
        HashMap<String, Object> requestMap = new HashMap<>();
        requestMap.put("title", "키보드팝니다");
        requestMap.put("description", "깨끗합니다");
        requestMap.put("minimumPrice", 10000);
        requestMap.put("location", "관악구 신림동");
        requestMap.put("imageUrl1", new String[]{"imageUrl1, imageUrl2"});

        String requestBody = objectMapper.writeValueAsString(requestMap);

        //when
        MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody);

        ResultActions response = mockMvc.perform(request);

        //then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("category 가 존재하지 않는다면")
    class ContextWithCategoryNotFound {

      @Test
      @DisplayName("BadRequest 를 응답한다")
      void ItResponseBadRequest() throws Exception {
        //given
        HashMap<String, Object> requestMap = new HashMap<>();
        requestMap.put("title", "키보드팝니다");
        requestMap.put("description", "깨끗합니다");
        requestMap.put("category", "존재하지 않는 카테고리");
        requestMap.put("minimumPrice", 10000);
        requestMap.put("location", "관악구 신림동");
        requestMap.put("imageUrl1", new String[]{"imageUrl1, imageUrl2"});

        String requestBody = objectMapper.writeValueAsString(requestMap);

        //when
        MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody);

        ResultActions response = mockMvc.perform(request);

        //then
        response.andExpect(status().isBadRequest());
      }
    }
  }

  @Nested
  @DisplayName("findById 메소드는")
  class DescribeFindById {

    @Nested
    @DisplayName("id에 숫자 외에 다른 문자가 들어온다면")
    class ContextNotNumberId {

      @Test
      @DisplayName("BadRequest로 응답한다.")
      void itResponseBadRequest() throws Exception {
        // given
        String inputId = "NotNumber";

        // when
        ResultActions response = mockMvc.perform(
            RestDocumentationRequestBuilders.get(BASE_URL + "/{id}", inputId));

        // then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("id에 음수가 들어온다면")
    class ContextNegativeNumberId {

      @Test
      @DisplayName("BadRequest로 응답한다.")
      void itResponseBadRequest() throws Exception {
        // given
        long inputId = -1;

        given(productService.findById(anyLong())).willThrow(IllegalArgumentException.class);

        // when
        ResultActions response = mockMvc.perform(
            RestDocumentationRequestBuilders.get(BASE_URL + "/{id}", inputId));

        // then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("정상적인 id가 들어온다면")
    class ContextValidId {

      @Test
      @DisplayName("해당 id를 가진 상품의 도메인 객체와 OK로 응답한다.")
      void itResponseOkWithProductDomainObjectHasInputId() throws Exception {
        // given
        long inputId = 1;
        User writer = new User("제로", "image", "google", "123", new Group());
        ReflectionTestUtils.setField(writer, "id", 1L);

        Product foundProduct = Product
            .builder()
            .title("귤 팔아요")
            .description("맛있어요")
            .category(FOOD)
            .images(Collections.emptyList())
            .location("제주도")
            .minimumPrice(1000)
            .writer(writer)
            .build();

        ReflectionTestUtils.setField(foundProduct, "id", inputId);
        ReflectionTestUtils.setField(foundProduct, "createdAt", LocalDateTime.now());

        Image image1 = Image
            .builder()
            .product(foundProduct)
            .url("image url1")
            .order(1)
            .build();
        Image image2 = Image
            .builder()
            .product(foundProduct)
            .url("image url2")
            .order(2)
            .build();
        List<Image> images = List.of(image1, image2);
        ReflectionTestUtils.setField(foundProduct, "images", images);

        ProductDetailResponse productDetailResponse = ProductDetailResponse.from(foundProduct);
        given(productService.findById(anyLong())).willReturn(productDetailResponse);

        // when
        ResultActions response = mockMvc.perform(
            RestDocumentationRequestBuilders.get(BASE_URL + "/{id}", inputId));

        // then
        verify(productService, atLeastOnce()).findById(anyLong());
        response
            .andExpect(status().isOk())
            .andDo(
                document(
                    "Find Product by id", preprocessResponse(prettyPrint()),
                    responseFields(
                        fieldWithPath("id")
                            .type(JsonFieldType.NUMBER)
                            .description("상품 식별자"),
                        fieldWithPath("title")
                            .type(JsonFieldType.STRING)
                            .description("상품 제목"),
                        fieldWithPath("description")
                            .type(JsonFieldType.STRING)
                            .description("상품 소개"),
                        fieldWithPath("minimumPrice")
                            .type(JsonFieldType.NUMBER)
                            .description("최소주문금액"),
                        fieldWithPath("category")
                            .type(JsonFieldType.STRING)
                            .description("카테고리 이름"),
                        fieldWithPath("location")
                            .type(JsonFieldType.STRING)
                            .description("거래 위치")
                            .optional(),
                        fieldWithPath("expireAt")
                            .type(JsonFieldType.STRING)
                            .description("비딩 종료 시간"),
                        fieldWithPath("createdAt")
                            .type(JsonFieldType.STRING)
                            .description("생성 시간"),
                        fieldWithPath("updatedAt")
                            .type(JsonFieldType.STRING)
                            .description("수정 시간")
                            .optional(),
                        fieldWithPath("writer.id")
                            .type(JsonFieldType.NUMBER)
                            .description("작성자 식별자"),
                        fieldWithPath("writer.username")
                            .type(JsonFieldType.STRING)
                            .description("작성자 이름"),
                        fieldWithPath("writer.profileImage")
                            .type(JsonFieldType.STRING)
                            .description("작성자 이미지 주소"),
                        fieldWithPath("images[].url")
                            .type(JsonFieldType.STRING)
                            .description("이미지 주소"),
                        fieldWithPath("images[].order")
                            .type(JsonFieldType.NUMBER)
                            .description("이미지 순서")
                    )
                ));
      }
    }
  }

  @Nested
  @DisplayName("findAll 메서드는")
  class DescribeFindAll {

    @Nested
    @DisplayName("유효한 값이 전달되면")
    class ContextWithValidData {
      @Test
      @DisplayName("상품을 조회하고 결과를 반환한다")
      void ItReturnProductList() throws Exception {
        //given
        List<ProductSelectResponse> responses = List.of(ProductSelectResponse.from(product));
        given(productService.findAll(any(ProductSelectRequest.class))).willReturn(responses);

        //when
        MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .get(BASE_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .queryParam("title", "null")
            .queryParam("progressed", "true")
            .queryParam("offset", "1")
            .queryParam("limit", "1")
            .queryParam("sort", Sort.END_DATE_ASC.name());

        ResultActions response = mockMvc.perform(request);

        //then
        verify(productService).findAll(any(ProductSelectRequest.class));
        response
            .andExpect(status().isOk())
            .andDo(document("Select product", preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestParameters(
                                parameterWithName("title")
                                    .description("상품 제목")
                                    .optional(),
                                parameterWithName("progressed")
                                    .description("상품 입찰 진행 여부")
                                    .optional(),
                                parameterWithName("offset").description("상품 조회 시작 번호"),
                                parameterWithName("limit").description("상품 조회 개수"),
                                parameterWithName("sort")
                                    .description("상품 정렬 기준")
                                    .optional()
                            ),
                            responseFields(
                                fieldWithPath("[].id")
                                    .type(JsonFieldType.NUMBER)
                                    .description("상품 식별자"),
                                fieldWithPath("[].title")
                                    .type(JsonFieldType.STRING)
                                    .description("상품 제목"),
                                fieldWithPath("[].thumbnailImage")
                                    .type(JsonFieldType.STRING)
                                    .description("상품 썸네일 이미지"),
                                fieldWithPath("[].minimumPrice")
                                    .type(JsonFieldType.NUMBER)
                                    .description("최소주문금액"),
                                fieldWithPath("[].heartCount")
                                    .type(JsonFieldType.NUMBER)
                                    .description("찜된 갯수"),
                                fieldWithPath("[].expireAt")
                                    .type(JsonFieldType.STRING)
                                    .description("비딩 종료 시간"),
                                fieldWithPath("[].createdAt")
                                    .type(JsonFieldType.STRING)
                                    .description("생성 시간"),
                                fieldWithPath("[].updatedAt")
                                    .type(JsonFieldType.STRING)
                                    .description("수정 시간")
                                    .optional()
                            )
            ));
      }
    }

    @Nested
    @DisplayName("progressed 에 boolean 외에 다른 문자가 들어온다면")
    class ContextNotBooleanProgressed {

      @Test
      @DisplayName("BadRequest 로 응답한다.")
      void itResponseBadRequest() throws Exception {
        // given
        String progressed = "NotBoolean";

        // when
        ResultActions response = mockMvc.perform(
            RestDocumentationRequestBuilders
                .get(BASE_URL)
                .param("progressed", progressed));

        // then
        response.andExpect(status().isBadRequest());
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
            RestDocumentationRequestBuilders
                .get(BASE_URL)
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
        ResultActions response = mockMvc.perform(
            RestDocumentationRequestBuilders
                .get(BASE_URL)
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
                                                     .get(BASE_URL)
                                                     .param(
                                                         "offset",
                                                         "1"
                                                     )
                                                     .param(
                                                         "limit",
                                                         limit
                                                     ));

        // then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("limit 에 음수 나 0이 들어온다면")
    class ContextNegativeOrZeroNumberLimit {

      @ParameterizedTest
      @ValueSource(strings = {"0", "-1"})
      @DisplayName("BadRequest 로 응답한다.")
      void itResponseBadRequest(String limit) throws Exception {
        // given
        // when
        ResultActions response = mockMvc.perform(RestDocumentationRequestBuilders
                                                     .get(BASE_URL)
                                                     .param(
                                                         "offset",
                                                         "1"
                                                     )
                                                     .param(
                                                         "limit",
                                                         limit
                                                     ));
        // then
        response.andExpect(status().isBadRequest());
      }
    }

    @Nested
    @DisplayName("title 이 길이 제한을 초과한다면")
    class ContextTitleOutOfLength {

      @Test
      @DisplayName("BadRequest 로 응답한다.")
      void itResponseBadRequest() throws Exception {
        // given
        // when
        ResultActions response = mockMvc.perform(RestDocumentationRequestBuilders
                                                     .get(BASE_URL)
                                                     .param(
                                                         "title",
                                                         "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                                                     )
                                                     .param(
                                                         "offset",
                                                         "1"
                                                     )
                                                     .param(
                                                         "limit",
                                                         "1"
                                                     ));
        // then
        response.andExpect(status().isBadRequest());
      }
    }
  }

  @Nested
  @DisplayName("getBiddingResult 메서드는")
  @WithMockCustomLoginUser
  class DescribeGetBiddingResult {

    @Nested
    @DisplayName("유효한 값이 전달되면")
    class ContextWithValidData {
      @Test
      @DisplayName("비딩 결과를 반환한다")
      void ItReturnBiddingResult() throws Exception {
        //given
        given(productService.getBiddingResult(anyLong(), anyLong()))
            .willReturn(BiddingResultResponse.responseForSuccessfulSeller(
                1,
                15000
            ));

        //when
        MockHttpServletRequestBuilder request = RestDocumentationRequestBuilders
            .get(BASE_URL + "/{productId}/result", productId);

        ResultActions response = mockMvc.perform(request);

        //then
        verify(productService).getBiddingResult(anyLong(), anyLong());
        response
            .andExpect(status().isOk())
            .andDo(document("Select bidding result", preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            pathParameters(
                                parameterWithName("productId").description("상품 아이디")
                            ),
                            responseFields(
                                fieldWithPath("role")
                                    .type(JsonFieldType.STRING)
                                    .description("유저 역할"),
                                fieldWithPath("biddingSucceed")
                                    .type(JsonFieldType.BOOLEAN)
                                    .description("비딩 성공 여부"),
                                fieldWithPath("chatRoomId")
                                    .type(JsonFieldType.NUMBER)
                                    .description("채팅방 아이디")
                                    .optional(),
                                fieldWithPath("winningPrice")
                                    .type(JsonFieldType.NUMBER)
                                    .description("낙찰 금액")
                                    .optional()
                            )
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
        ResultActions response = mockMvc.perform(
            RestDocumentationRequestBuilders.get(BASE_URL + "/{productId}/result", productId));

        // then
        response.andExpect(status().isBadRequest());
      }
    }
  }
}
