package com.leiyang.anontask.dto.mp;

import java.time.Instant;
import java.util.List;

public record PlazaPostItem(
    long id,
    long authorId,
    String authorName,
    String authorAvatar,
    String gender,
    String category,
    String categoryName,
    String content,
    List<String> images,
    int likeCount,
    int commentCount,
    boolean liked,
    boolean followed,
    boolean hot,
    Instant createdAt,
    MusicAttachment music
) {
  public record MusicAttachment(
      long id,
      String title,
      String audioUrl,
      String imageUrl,
      String videoUrl,
      String duration,
      String authorName,
      String lyricist,
      String composer,
      String lyrics,
      String style,
      boolean deleted
  ) {}
}
