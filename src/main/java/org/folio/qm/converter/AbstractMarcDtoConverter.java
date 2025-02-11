package org.folio.qm.converter;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

import static org.folio.qm.converter.elements.Constants.ADDITIONAL_CHARACTERISTICS_CONTROL_FIELD;
import static org.folio.qm.converter.elements.Constants.BLANK_REPLACEMENT;
import static org.folio.qm.converter.elements.Constants.GENERAL_INFORMATION_CONTROL_FIELD;
import static org.folio.qm.converter.elements.Constants.PHYSICAL_DESCRIPTIONS_CONTROL_FIELD;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.marc4j.MarcJsonReader;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.springframework.lang.NonNull;

import org.folio.qm.converter.elements.AdditionalMaterialConfiguration;
import org.folio.qm.converter.elements.Constants;
import org.folio.qm.converter.elements.ControlFieldItem;
import org.folio.qm.converter.elements.PhysicalDescriptionFixedFieldElements;
import org.folio.qm.domain.dto.FieldItem;
import org.folio.qm.domain.dto.QuickMarc;
import org.folio.qm.domain.dto.UpdateInfo;
import org.folio.qm.exception.ConverterException;
import org.folio.rest.jaxrs.model.ParsedRecord;
import org.folio.rest.jaxrs.model.ParsedRecordDto;

public abstract class AbstractMarcDtoConverter implements MarcDtoConverter {

  @Override
  public QuickMarc convert(@NonNull ParsedRecordDto source) {
    ParsedRecord parsedRecord = source.getParsedRecord();

    try (InputStream input = IOUtils
      .toInputStream(new ObjectMapper().writeValueAsString(parsedRecord), StandardCharsets.UTF_8)) {
      Record marcRecord = new MarcJsonReader(input).next();
      String leader = masqueradeBlanks(marcRecord.getLeader().marshal());

      List<FieldItem> fields = marcRecord.getControlFields()
        .stream()
        .map(cf -> controlFieldToQuickMarcField(cf, leader))
        .collect(Collectors.toList());

      fields.addAll(marcRecord.getDataFields()
        .stream()
        .map(this::dataFieldToQuickMarcField)
        .collect(Collectors.toList()));

      return new QuickMarc().parsedRecordId(UUID.fromString(parsedRecord.getId()))
        .leader(leader)
        .fields(fields)
        .parsedRecordDtoId(UUID.fromString(source.getId()))
        .externalId(getExternalId(source))
        .externalHrid(getExternalHrId(source))
        .marcFormat(supportedType())
        .suppressDiscovery(source.getAdditionalInfo().getSuppressDiscovery())
        .updateInfo(new UpdateInfo()
          .recordState(UpdateInfo.RecordStateEnum.fromValue(source.getRecordState().value()))
          .updateDate(convertDate(source)));
    } catch (Exception e) {
      throw new ConverterException(e);
    }
  }

  protected abstract UUID getExternalId(ParsedRecordDto source);
  protected abstract String getExternalHrId(ParsedRecordDto source);
  protected abstract Map<String, Object> splitGeneralInformationControlField(String content, String leader);

  private OffsetDateTime convertDate(ParsedRecordDto parsedRecordDto) {
    var updatedDate = parsedRecordDto.getMetadata().getUpdatedDate();
    return updatedDate != null ? OffsetDateTime.ofInstant(updatedDate.toInstant(), ZoneId.systemDefault()) : null;
  }

  private Object processControlField(ControlField controlField, String leader) {
    switch (controlField.getTag()) {
      case ADDITIONAL_CHARACTERISTICS_CONTROL_FIELD:
        return splitAdditionalCharacteristicsControlField(masqueradeBlanks(controlField.getData()));
      case PHYSICAL_DESCRIPTIONS_CONTROL_FIELD:
        return splitPhysicalDescriptionsControlField(masqueradeBlanks(controlField.getData()));
      case GENERAL_INFORMATION_CONTROL_FIELD:
        return splitGeneralInformationControlField(masqueradeBlanks(controlField.getData()), leader);
      default:
        return controlField.getData();
    }
  }

  private Map<String, Object> splitAdditionalCharacteristicsControlField(String content) {
    return fillContentMap(AdditionalMaterialConfiguration.resolveByCode(content.charAt(0)).getControlFieldItems(), content,
      0);
  }

  private Map<String, Object> splitPhysicalDescriptionsControlField(String content) {
    var physicalDescription = PhysicalDescriptionFixedFieldElements
      .resolveByCode(content.charAt(0));
    Map<String, Object> contentMap = new LinkedHashMap<>();
    contentMap.put(Constants.CATEGORY_NAME, physicalDescription.getName());
    physicalDescription.getControlFieldItems()
      .forEach(item -> contentMap.put(item.getName(), getControlFieldItemVal(content, item)));
    return contentMap;
  }

  private String getControlFieldItemVal(String content, ControlFieldItem element) {
    return element.getLength() != 0 ? extractElementFromContent(content, element, 0) : content;
  }

  protected Map<String, Object> fillContentMap(List<ControlFieldItem> items, String content, int delta) {
    return items.stream()
      .collect(
        toMap(ControlFieldItem::getName, element -> getControlFieldElementContent(content, element, delta), (o, o2) -> o,
          LinkedHashMap::new));
  }

  private Object getControlFieldElementContent(String content, ControlFieldItem element, int delta) {
    var elementFromContent = extractElementFromContent(content, element, delta);
    return element.isArray()
      ? Arrays.asList(elementFromContent.split(EMPTY))
      : elementFromContent;
  }

  private String extractElementFromContent(String content, ControlFieldItem element, int delta) {
    return StringUtils
      .substring(content, element.getPosition() + delta, element.getPosition() + delta + element.getLength());
  }

  private FieldItem dataFieldToQuickMarcField(DataField dataField) {
    return new FieldItem().tag(dataField.getTag())
      .indicators(Arrays.asList(masqueradeBlanks(Character.toString(dataField.getIndicator1())),
        masqueradeBlanks(Character.toString(dataField.getIndicator2()))))
      .content(dataField.getSubfields()
        .stream()
        .map(subfield -> new StringBuilder("$").append(subfield.getCode())
          .append(SPACE)
          .append(subfield.getData()))
        .collect(Collectors.joining(SPACE)));
  }

  private FieldItem controlFieldToQuickMarcField(ControlField cf, String leader) {
    return new FieldItem()
        .tag(cf.getTag())
        .content(processControlField(cf, leader))
        .indicators(Collections.emptyList());
  }

  private String masqueradeBlanks(String sourceString) {
    return sourceString.replace(SPACE, BLANK_REPLACEMENT);
  }

}
