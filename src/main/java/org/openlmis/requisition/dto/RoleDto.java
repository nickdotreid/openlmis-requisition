package org.openlmis.requisition.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class RoleDto {
  private UUID id;
  private String name;
  private String description;
  private Set<RightDto> rights;
}
