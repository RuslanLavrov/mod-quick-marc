package org.folio.converter;

import static org.folio.converter.FixedLengthControlFieldItems.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum PhysicalDescriptions {
  ELECTRONIC_RESOURCE("Electronic resource", 'c', 14, Arrays.asList(SMD, COLOR, DIMENSIONS, SOUND, IMAGE_BIT_DEPTH, FILE_FORMATS, QUALITY_ASSURANCE_TARGET, ANTECEDENT, LEVEL_OF_COMPRESSION, REFORMATTING_QUALITY)),
  GLOBE("Globe", 'd', 6, Arrays.asList(SMD, COLOR, PHYSICAL_MEDIUM, TYPE_OF_REPRODUCTION)),
  KIT("Kit", 'o', 2, Collections.singletonList(SMD)),
  MAP("Map", 'a', 8, Arrays.asList(SMD, COLOR, PHYSICAL_MEDIUM, TYPE_OF_REPRODUCTION, PRODUCTION_DETAILS, ASPECT_MAP)),
  MICROFORM("Microform", 'h', 13, Arrays.asList(SMD, ASPECT_MICROFORM, DIMENSIONS, REDUCTION_RATIO, COLOR_MICROFORM, EMULSION_ON_FILM, GENERATION, BASE_OF_FILM)),
  MOTION_PICTURE("Motion Picture", 'm', 23, Arrays.asList(SMD, COLOR, MOTION_PICTURE_PRESENTATION_FORMAT, SOUND_ON_MEDIUM, MEDIUM_FOR_SOUND, DIMENSIONS_MOTION, CONFIGURATION_OF_CHANNELS, PRODUCTION_ELEMENTS, ASPECT_MOTION, GENERATION, BASE_OF_FILM, REFINED_CATEGORIES, KIND_OF_COLOR, DETERIORATION_STAGE, COMPLETENESS, FILM_INSPECTION_DATE)),
  NONPROJECTED_GRAPHIC("Nonprojected Graphic", 'k', 6, Arrays.asList(SMD, COLOR, PRIMARY_SUPPORT, SECONDARY_SUPPORT)),
  NOTATED_MUSIC("Notated Music", 'q', 2, Collections.singletonList(SMD)),
  PROJECTED_GRAPHIC("Projected Graphic", 'g', 9, Arrays.asList(SMD, COLOR, BASE_OF_EMULSION, SOUND_ON_MEDIUM, MEDIUM_FOR_SOUND, DIMENSIONS_MOTION, SECONDARY_SUPPORT_PROJECTED)),
  REMOTE_SENSING_IMAGE("Remote-Sensing Image", 'r', 11, Arrays.asList(SMD, ALTITUDE, ATTITUDE, CLOUD_COVER, PLATFORM_CONSTRUCTION_TYPE, PLATFORM_USE_CATEGORY, SENSOR_TYPE, DATA_TYPE)),
  SOUND_RECORDING("Sound Recording", 's', 14, Arrays.asList(SMD, SPEED, CONFIGURATION_OF_CHANNELS_SOUND, GROOVE, DIMENSIONS_SOUND, TAPE_WIDTH, TAPE_CONFIGURATION, KIND_OF_DISC, KIND_OF_MATERIAL, KIND_OF_CUTTING, SPECIAL_PLAYBACK_CHARACTERISTICS, CAPTURE_TECHNIQUE)),
  TACTILE_MATERIAL("Tactile Material", 'f', 10, Arrays.asList(SMD, CLASS_OF_BRAILLE, LEVEL_OF_CONTRACTION, BRAILLE_MUSIC_FORMAT, SPECIAL_PHYSICAL_CHARACTERISTICS)),
  TEXT("Text", 't', 2, Collections.singletonList(SMD)),
  UNSPECIFIED("Unspecified", 'z', 2, Collections.singletonList(SMD)),
  VIDEORECORDING("Videorecording", 'v', 9, Arrays.asList(SMD, COLOR, VIDEORECORDING_FORMAT, SOUND_ON_MEDIUM, MEDIUM_FOR_SOUND, DIMENSIONS_MOTION, CONFIGURATION_OF_CHANNELS)),
  UNKNOWN("Unknown", 'b', 1, Collections.singletonList(VAL));

  private String name;
  private char code;
  private int length;
  private List<FixedLengthControlFieldItems> items;

  PhysicalDescriptions(String name, char code, int length, List<FixedLengthControlFieldItems> items) {
    this.name = name;
    this.code = code;
    this.length = length;
    this.items = items;
  }

  public static PhysicalDescriptions resolveByCode(char code) {
    for(PhysicalDescriptions physicalDescriptions : values()) {
      if (physicalDescriptions.code == code) {
        return physicalDescriptions;
      }
    }
    return UNKNOWN;
  }

  public String getName() {
    return name;
  }

  public int getLength() {
    return length;
  }

  public List<FixedLengthControlFieldItems> getItems() {
    return items;
  }
}
