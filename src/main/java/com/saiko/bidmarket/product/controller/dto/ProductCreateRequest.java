package com.saiko.bidmarket.product.controller.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.saiko.bidmarket.product.Category;

public class ProductCreateRequest {
  @NotBlank
  @Length(max = 16)
  private final String title;

  @NotBlank
  @Length(max = 500)
  private final String description;

  @Size(max = 5)
  private final List<String> images = new ArrayList<>();

  @NotNull
  private final Category category;

  @Min(value = 1000)
  private final int minimumPrice;

  private final String location;

  @JsonCreator
  public ProductCreateRequest(@JsonProperty("title") String title,
                              @JsonProperty("description") String description,
                              @JsonProperty("category") Category category,
                              @JsonProperty("minimumPrice") int minimumPrice,
                              @JsonProperty("location") String location) {
    this.title = title;
    this.description = description;
    this.category = category;
    this.minimumPrice = minimumPrice;
    this.location = location;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getImages() {
    return images;
  }

  public Category getCategory() {
    return category;
  }

  public int getMinimumPrice() {
    return minimumPrice;
  }

  public String getLocation() {
    return location;
  }
}
