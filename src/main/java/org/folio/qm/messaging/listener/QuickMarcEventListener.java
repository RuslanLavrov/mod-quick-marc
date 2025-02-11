package org.folio.qm.messaging.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import org.folio.qm.messaging.domain.QmCompletedEventPayload;
import org.folio.qm.service.CacheService;
import org.folio.qm.util.ErrorUtils;
import org.folio.tenant.domain.dto.Error;

@Log4j2
@Component
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class QuickMarcEventListener {

  private final CacheService<DeferredResult> cacheService;
  private final ObjectMapper objectMapper;

  @KafkaListener(groupId = "#{@groupIdProvider.qmCompletedGroupId()}",
    topicPattern = "#{@topicPatternProvider.qmCompletedTopicName()}",
    containerFactory = "quickMarcKafkaListenerContainerFactory")
  public void qmCompletedListener(QmCompletedEventPayload data) throws JsonProcessingException {
    var recordId = data.getRecordId();
    log.info("QM_COMPLETED received for record id [{}]", recordId);
    DeferredResult deferredResult = cacheService.getFromCache(String.valueOf(recordId));
    if (deferredResult != null) {
      if (data.isSucceed()) {
        deferredResult.setResult(ResponseEntity.accepted().build());
      } else {
        var errorMessage = data.getErrorMessage();
        if (isOptimisticLockingError(errorMessage)) {
          deferredResult.setErrorResult(buildOptimisticLockingErrorResponse(errorMessage));
        } else {
          ResponseEntity<Error> body = buildCommonErrorResponse(errorMessage);
          deferredResult.setErrorResult(body);
        }
      }
      cacheService.invalidate(String.valueOf(recordId));
    }
  }

  @NotNull
  private ResponseEntity<Error> buildCommonErrorResponse(String errorMessage) {
    var error = ErrorUtils.buildError(ErrorUtils.ErrorType.EXTERNAL_OR_UNDEFINED, errorMessage);
    return ResponseEntity.badRequest().body(error);
  }

  @NotNull
  private ResponseEntity<Error> buildOptimisticLockingErrorResponse(String errorMessage) throws JsonProcessingException {
    var errorNode = objectMapper.readTree(errorMessage);
    var message = errorNode.get("message").asText();
    var error = ErrorUtils.buildError(ErrorUtils.ErrorType.EXTERNAL_OR_UNDEFINED, message);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  private boolean isOptimisticLockingError(String errorMessage) {
    return errorMessage.contains("(optimistic locking)");
  }
}
