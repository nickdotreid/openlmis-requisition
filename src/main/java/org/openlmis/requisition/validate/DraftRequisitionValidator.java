package org.openlmis.requisition.validate;

import static org.springframework.util.CollectionUtils.isEmpty;

import org.openlmis.requisition.domain.Requisition;
import org.openlmis.requisition.domain.RequisitionLineItem;
import org.openlmis.requisition.domain.RequisitionStatus;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.exception.RequisitionTemplateColumnException;
import org.openlmis.requisition.repository.RequisitionTemplateRepository;
import org.openlmis.settings.service.ConfigurationSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class DraftRequisitionValidator implements Validator {

  @Autowired
  private RequisitionTemplateRepository requisitionTemplateRepository;

  @Autowired
  private ConfigurationSettingService configurationSettingService;

  @Override
  public boolean supports(Class<?> clazz) {
    return Requisition.class.equals(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    Requisition requisition = (Requisition) target;

    if (!isEmpty(requisition.getRequisitionLineItems())) {
      RequisitionTemplate template = requisitionTemplateRepository.getTemplateForProgram(
          requisition.getProgramId()
      );
      requisition.getRequisitionLineItems()
          .forEach(i -> validateRequisitionLineItem(errors, template, requisition, i));
    }
  }

  private void validateRequisitionLineItem(Errors errors, RequisitionTemplate template,
                                           Requisition requisition, RequisitionLineItem item) {
    rejectIfCalculatedAndNotNull(errors, template, item.getStockOnHand(),
        RequisitionValidator.STOCK_ON_HAND);
    rejectIfCalculatedAndNotNull(errors, template, item.getTotalConsumedQuantity(),
        RequisitionValidator.TOTAL_CONSUMED_QUANTITY);

    validateApprovalFields(errors, requisition, item);
  }

  private void validateApprovalFields(Errors errors, Requisition requisition,
                                      RequisitionLineItem item) {
    RequisitionStatus expectedStatus;
    if (configurationSettingService.getBoolValue("skipAuthorization")) {
      expectedStatus = RequisitionStatus.SUBMITTED;
    } else {
      expectedStatus = RequisitionStatus.AUTHORIZED;
    }
    rejectIfInvalidStatusAndNotNull(errors, requisition, item.getApprovedQuantity(),
        expectedStatus, RequisitionValidator.APPROVED_QUANTITY
            + RequisitionValidator.IS_ONLY_AVAILABLE_DURING_APPROVAL_STEP);

    rejectIfInvalidStatusAndNotNull(errors, requisition, item.getRemarks(),
        expectedStatus, RequisitionValidator.REMARKS
            + RequisitionValidator.IS_ONLY_AVAILABLE_DURING_APPROVAL_STEP);

  }

  private void rejectIfCalculatedAndNotNull(Errors errors, RequisitionTemplate template,
                                            Object value, String field) {
    try {
      if (template.isColumnCalculated(field) && value != null) {
        errors.rejectValue(RequisitionValidator.REQUISITION_LINE_ITEMS,
            field + RequisitionValidator.TEMPLATE_COLUMN_IS_CALCULATED);
      }
    } catch (RequisitionTemplateColumnException ex) {
      errors.rejectValue(RequisitionValidator.REQUISITION_LINE_ITEMS, ex.getMessage());
    }
  }

  private void rejectIfInvalidStatusAndNotNull(Errors errors, Requisition requisition, Object value,
                                               RequisitionStatus expectedStatus, String errorCode) {
    if (requisition.getStatus() != expectedStatus && value != null) {
      errors.rejectValue(RequisitionValidator.REQUISITION_LINE_ITEMS, errorCode);
    }
  }
}
