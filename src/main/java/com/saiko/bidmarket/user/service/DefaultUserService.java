package com.saiko.bidmarket.user.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.saiko.bidmarket.bidding.repository.BiddingRepository;
import com.saiko.bidmarket.common.exception.NotFoundException;
import com.saiko.bidmarket.heart.entity.Heart;
import com.saiko.bidmarket.heart.repository.HeartRepository;
import com.saiko.bidmarket.product.entity.Product;
import com.saiko.bidmarket.product.repository.ProductRepository;
import com.saiko.bidmarket.product.repository.dto.UserProductSelectQueryParameter;
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
import com.saiko.bidmarket.user.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class DefaultUserService implements UserService {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final ProductRepository productRepository;
  private final BiddingRepository biddingRepository;
  private final UserRepository userRepository;
  private final HeartRepository heartRepository;
  private final GroupService groupService;

  @Override
  public User findByProviderAndProviderId(String provider, String providerId) {
    Assert.hasText(provider, "Provider must be provided");
    Assert.hasText(providerId, "ProviderId must be provided");

    return userRepository.findByProviderAndProviderId(provider, providerId)
                         .orElseThrow(() -> new NotFoundException("User does not exist"));
  }

  @Override
  @Transactional
  public User join(OAuth2User oAuth2User, String authorizedClientRegistrationId) {
    Assert.notNull(oAuth2User, "OAuth2User must be provided");
    Assert.hasText(authorizedClientRegistrationId,
                   "AuthorizedClientRegistrationId must be provided");

    String providerId = oAuth2User.getName();
    try {
      User user = findByProviderAndProviderId(authorizedClientRegistrationId, providerId);
      log.warn("Already exists: {} for (provider: {}, providerId: {})", user,
               authorizedClientRegistrationId, providerId);
      return user;
    } catch (NotFoundException e) {
      Map<String, Object> attributes = oAuth2User.getAttributes();

      String username = (String)attributes.get("name");
      String profileImage = (String)attributes.get("picture");

      log.info("username : {} profileImage : {}", username, profileImage);
      Group group = groupService.findByName("USER_GROUP");

      User user = new User(username, profileImage, authorizedClientRegistrationId, providerId,
                           group);
      return userRepository.save(user);
    }
  }

  @Override
  public UserSelectResponse findById(long id) {
    Assert.isTrue(id > 0, "userId must be positive");

    return UserSelectResponse.from(userRepository.findById(id)
                                                 .orElseThrow(() -> new NotFoundException(
                                                     "User does not exist")));
  }

  @Override
  @Transactional
  public void updateUser(long id, UserUpdateRequest request) {
    Assert.notNull(request, "request must be provide");

    final User user = userRepository.findById(id)
                                    .orElseThrow(
                                        () -> new NotFoundException("User does not exist"));
    user.update(request.getUsername(), request.getProfileImage());
  }

  @Override
  public List<UserProductSelectResponse> findAllUserProducts(
      long userId,
      UserProductSelectRequest request
  ) {
    Assert.isTrue(userId > 0, "User id must be positive");
    Assert.notNull(request, "Request must be provided");

    final UserProductSelectQueryParameter queryParameter = UserProductSelectQueryParameter.of(
        userId, request);

    return productRepository.findAllUserProduct(queryParameter)
                            .stream()
                            .map(UserProductSelectResponse::from)
                            .collect(Collectors.toList());
  }

  @Override
  public List<UserBiddingSelectResponse> findAllUserBiddings(
      long userId,
      UserBiddingSelectRequest request
  ) {
    Assert.notNull(request, "Request must be provided");

    return biddingRepository
        .findAllUserBidding(userId, request)
        .stream()
        .map((bidding) -> bidding.getProduct())
        .map(UserBiddingSelectResponse::from)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void deleteUser(long userId) {
    Assert.isTrue(userId > 0, "User id must be positive");

    final User user = userRepository
        .findById(userId)
        .orElseThrow(() -> new  NotFoundException("User does not exist"));

    biddingRepository.deleteAllBatchByBidderId(userId);
    finishUserProducts(userId);
    user.delete();
    //TODO: productRepository를 변경하도록 수정해야함
    userRepository.save(user);
  }

  @Override
  @Transactional
  public void toggleHeart(
      long userId,
      long productId
  ) {
    User user = userRepository
        .findById(userId)
        .orElseThrow(() -> new NotFoundException("Product does not exist"));

    Product product = productRepository
        .findById(productId)
        .orElseThrow(() -> new NotFoundException("Product does not exist"));

    Heart heart = findHeart(user, product);
    heart.toggle();
  }

  @Override
  public List<UserHeartSelectResponse> findAllUserHearts(
      long userId,
      UserHeartSelectRequest request
  ) {
    Assert.notNull(request, "Request must be provided");

    return heartRepository.findAllUserHeart(userId, request)
                            .stream()
                            .map(Heart::getProduct)
                            .map(UserHeartSelectResponse::from)
                            .collect(Collectors.toList());
  }

  @Override
  public UserHeartCheckResponse isUserHearts(
      long userId,
      long productId
  ) {

    productRepository
        .findById(productId)
        .orElseThrow(() -> new NotFoundException("Product does not exist"));

    return heartRepository
        .findByUserIdAndProductId(userId, productId)
        .map(Heart::isActived)
        .map(UserHeartCheckResponse::from)
        .orElseGet(() -> UserHeartCheckResponse.from(false));
  }

  private Heart findHeart(
      User user,
      Product product
  ) {
    return heartRepository
        .findByUserAndProduct(user, product)
        .orElseGet(() -> heartRepository.save(Heart.of(user, product)));
  }

  private void finishUserProducts(long userId) {
    productRepository
        .findAllByWriterIdAndProgressed(userId, true)
        .forEach(product -> biddingRepository.deleteAllBatchByProductId(product.getId()));
    productRepository.finishByUserId(userId);
  }
}
