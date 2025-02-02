package com.saiko.bidmarket.chat.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.saiko.bidmarket.chat.controller.dto.ChatRoomSelectRequest;
import com.saiko.bidmarket.chat.entity.ChatRoom;
import com.saiko.bidmarket.common.config.QueryDslConfig;
import com.saiko.bidmarket.product.Category;
import com.saiko.bidmarket.product.entity.Product;
import com.saiko.bidmarket.product.repository.ProductRepository;
import com.saiko.bidmarket.user.entity.Group;
import com.saiko.bidmarket.user.entity.User;
import com.saiko.bidmarket.user.repository.GroupRepository;
import com.saiko.bidmarket.user.repository.UserRepository;

@DataJpaTest()
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(value = QueryDslConfig.class)
public class ChatRoomRepositoryTest {

  @Autowired
  private ChatRoomRepository chatRoomRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private GroupRepository groupRepository;

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void deleteAll() {
    chatRoomRepository.deleteAll();
    productRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Nested
  @DisplayName("findByProduct_IdAndSeller_Id 메소드는")
  class DescribeFindByProduct_IdAndSeller_Id {

    @Test
    @DisplayName("판매자와 낙찰자가 참여한 채팅방을 반환한다")
    void ItReturnChatRoom() {
      //given
      @SuppressWarnings({"all"})
      Group group = groupRepository
          .findById(1L)
          .get();
      User seller = userRepository.save(getUser("1234", group));
      User winner = userRepository.save(getUser("123", group));

      Product product = productRepository.save(getProduct(seller));
      ChatRoom chatRoom = chatRoomRepository.save(getChatRoom(seller, winner, product));

      //when
      @SuppressWarnings({"all"})
      ChatRoom foundChatRoom = chatRoomRepository
          .findByProduct_IdAndSeller_Id(
              product.getId(),
              seller.getId()
          )
          .get();

      //then
      assertThat(foundChatRoom).isEqualTo(chatRoom);
    }
  }

  @Nested
  @DisplayName("findAllByUserId 메서드는")
  class DescribeFindAllByUserId {

    @Test
    @DisplayName("해당 유저가 속해있는 모든 채팅방을 반환한다")
    void ItReturnAllChatRoomThatJoinInto() {
      //given
      @SuppressWarnings("all")
      Group group = groupRepository
          .findById(1L)
          .get();

      User user1 = userRepository.save(getUser("1", group));
      User user2 = userRepository.save(getUser("2", group));
      User user3 = userRepository.save(getUser("3", group));

      Product product1 = productRepository.save(getProduct(user1));
      Product product2 = productRepository.save(getProduct(user2));
      Product product3 = productRepository.save(getProduct(user1));

      chatRoomRepository.save(getChatRoom(user1, user2, product1));
      chatRoomRepository.save(getChatRoom(user2, user3, product2));
      chatRoomRepository.save(getChatRoom(user1, user3, product3));

      ChatRoomSelectRequest request = new ChatRoomSelectRequest(0, 10);

      //when
      List<ChatRoom> chatRooms = chatRoomRepository.findAllByUserId(user2.getId(), request);

      //then
      assertThat(chatRooms.size()).isEqualTo(2);
    }
  }

  private User getUser(
      String providerId,
      Group group
  ) {
    return User
        .builder()
        .username("제로")
        .group(group)
        .profileImage("image")
        .provider("google")
        .providerId(providerId)
        .build();
  }

  private Product getProduct(User seller) {
    return Product
        .builder()
        .title("코드 리뷰 해드려요")
        .description("좋아요")
        .category(Category.HOBBY)
        .location("대면은 안해요")
        .images(List.of("image"))
        .minimumPrice(10000)
        .writer(seller)
        .build();
  }

  private ChatRoom getChatRoom(
      User seller,
      User winner,
      Product product
  ) {
    return ChatRoom
        .builder()
        .seller(seller)
        .product(product)
        .winner(winner)
        .build();
  }

}
