package com.leiyang.anontask.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leiyang.anontask.domain.AiMusicJob;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class AiMusicServiceTest {
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void parsesSunoDataFieldsFromRecordInfoResponse() throws Exception {
    AiMusicService service = new AiMusicService(
        null, null, null, null, mapper, null, null, null, null, null, null, null
    );
    AiMusicJob job = new AiMusicJob();
    JsonNode root = mapper.readTree("""
        {
          "code": 200,
          "data": {
            "taskId": "task-1",
            "response": {
              "sunoData": [
                {
                  "id": "audio-1",
                  "audioUrl": "https://example.com/a.mp3",
                  "imageUrl": "https://example.com/a.jpg",
                  "prompt": "[Verse]\\n新的歌词",
                  "title": "三方标题",
                  "tags": "Piano ballad, Mandarin",
                  "duration": 232.8
                }
              ]
            },
            "status": "SUCCESS"
          }
        }
        """);

    Method apply = AiMusicService.class.getDeclaredMethod("applyMusicPayload", AiMusicJob.class, JsonNode.class);
    apply.setAccessible(true);
    apply.invoke(service, job, root.path("data"));

    assertEquals("SUCCESS", job.getStatus());
    assertEquals("https://example.com/a.mp3", job.getAudioUrl());
    assertEquals("https://example.com/a.jpg", job.getImageUrl());
    assertEquals("232.8", job.getDuration());
    assertEquals("audio-1", job.getSunoAudioId());
    assertEquals("[Verse]\n新的歌词", job.getLyrics());
    assertEquals("Piano ballad, Mandarin", job.getStyle());
  }

  @Test
  void parsesSunoCallbackDataArrayFields() throws Exception {
    AiMusicService service = new AiMusicService(
        null, null, null, null, mapper, null, null, null, null, null, null, null
    );
    AiMusicJob job = new AiMusicJob();
    JsonNode root = mapper.readTree("""
        {
          "code": 200,
          "data": {
            "callbackType": "complete",
            "task_id": "task-1",
            "data": [
              {
                "id": "audio-2",
                "audio_url": "https://example.com/b.mp3",
                "image_url": "https://example.com/b.jpg",
                "prompt": "[Chorus]\\n回调歌词",
                "tags": "Rock, Joyful",
                "duration": 188.42
              }
            ]
          }
        }
        """);

    Method apply = AiMusicService.class.getDeclaredMethod("applyMusicPayload", AiMusicJob.class, JsonNode.class);
    apply.setAccessible(true);
    apply.invoke(service, job, root.path("data"));

    assertEquals("SUCCESS", job.getStatus());
    assertEquals("https://example.com/b.mp3", job.getAudioUrl());
    assertEquals("https://example.com/b.jpg", job.getImageUrl());
    assertEquals("188.42", job.getDuration());
    assertEquals("audio-2", job.getSunoAudioId());
    assertEquals("[Chorus]\n回调歌词", job.getLyrics());
    assertEquals("Rock, Joyful", job.getStyle());
  }

  @Test
  void textCallbackDoesNotMarkJobCompleteOrRequireDuration() throws Exception {
    AiMusicService service = new AiMusicService(
        null, null, null, null, mapper, null, null, null, null, null, null, null
    );
    AiMusicJob job = new AiMusicJob();
    job.setStatus("SUBMITTED");
    JsonNode root = mapper.readTree("""
        {
          "data": {
            "callbackType": "TEXT",
            "task_id": "task-1",
            "data": [
              {
                "audio_url": "https://example.com/text-stage.mp3"
              }
            ]
          }
        }
        """);

    Method apply = AiMusicService.class.getDeclaredMethod("applyMusicPayload", AiMusicJob.class, JsonNode.class);
    apply.setAccessible(true);
    apply.invoke(service, job, root.path("data"));

    assertEquals("PROCESSING", job.getStatus());
    assertEquals("https://example.com/text-stage.mp3", job.getAudioUrl());
    assertEquals(null, job.getDuration());
  }

  @Test
  void firstCallbackWithDurationMarksJobSuccess() throws Exception {
    AiMusicService service = new AiMusicService(
        null, null, null, null, mapper, null, null, null, null, null, null, null
    );
    AiMusicJob job = new AiMusicJob();
    JsonNode root = mapper.readTree("""
        {
          "data": {
            "callbackType": "FIRST",
            "task_id": "task-1",
            "data": [
              {
                "audio_url": "https://example.com/first.mp3",
                "duration": 139.84
              }
            ]
          }
        }
        """);

    Method apply = AiMusicService.class.getDeclaredMethod("applyMusicPayload", AiMusicJob.class, JsonNode.class);
    apply.setAccessible(true);
    apply.invoke(service, job, root.path("data"));

    assertEquals("SUCCESS", job.getStatus());
    assertEquals("https://example.com/first.mp3", job.getAudioUrl());
    assertEquals("139.84", job.getDuration());
  }
}
