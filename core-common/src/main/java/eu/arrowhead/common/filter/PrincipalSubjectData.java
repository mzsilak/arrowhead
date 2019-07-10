/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */
package eu.arrowhead.common.filter;

import eu.arrowhead.common.misc.SecurityUtils;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class PrincipalSubjectData {


  public enum SubjectFields {
    FULL_NAME, COMMON_NAME, CLOUD_NAME, OPERATOR, ARROWHEAD, SUFFIX // Suffix is everything after the common name
  }

  public static class CertificateFragment {

    private final SubjectFields field;
    private final String value;

    public CertificateFragment(final SubjectFields field, final String value) {
      this.field = field;
      this.value = value;
    }

    public SubjectFields getField() {
      return field;
    }

    public String getValue() {
      return value;
    }
  }

  private final boolean present;
  private final Map<SubjectFields, String> subjectMap = new HashMap<>();

  public PrincipalSubjectData(final Principal principal) {
    this(SecurityUtils.getCertCNFromSubject(principal.getName()));
  }

  public PrincipalSubjectData(final String subject) {

    present = subject != null;
    if (!present) {
      return;
    }

    String[] fields = subject.split("\\.", 4);

    subjectMap.put(SubjectFields.FULL_NAME, subject);

    if (fields.length > 0) {
      subjectMap.put(SubjectFields.COMMON_NAME, fields[0]);
    }
    if (fields.length > 1) {
      subjectMap.put(SubjectFields.CLOUD_NAME, fields[1]);
    }
    if (fields.length > 2) {
      subjectMap.put(SubjectFields.OPERATOR, fields[2]);
    }
    if (fields.length > 3) {
      subjectMap.put(SubjectFields.ARROWHEAD, fields[3]);
    }

    fields = subject.split("\\.", 2);
    subjectMap.put(SubjectFields.SUFFIX, fields[1]);
  }

  public boolean isPresent() {
    return present;
  }

  public String getSubjectValue(final SubjectFields field) {
    return subjectMap.getOrDefault(field, "");
  }

  public String getSubject() {
    return subjectMap.getOrDefault(SubjectFields.FULL_NAME, "");
  }

  public String getCommonName() {
    return subjectMap.getOrDefault(SubjectFields.COMMON_NAME, "");
  }

  public String getSuffix() {
    return subjectMap.getOrDefault(SubjectFields.SUFFIX, "");
  }

  public boolean equals(final PrincipalSubjectData other, final SubjectFields... fields) {

    boolean retValue = true;
    for (SubjectFields field : fields) {
      final String thisValue = this.getSubjectValue(field);
      final String otherValue = other.getSubjectValue(field);
      retValue &= thisValue.equals(otherValue);
    }

    return retValue;
  }

  public boolean equals(final CertificateFragment fragment) {
    final String thisValue = getSubjectValue(fragment.getField());
    return thisValue.equals(fragment.getValue());
  }

  public boolean equals(final PrincipalSubjectData other) {
    return this.getSubject().equals(other.getSubject());
  }

  public PrincipalSubjectData createWithSuffix(final String commonName) {
    PrincipalSubjectData data = new PrincipalSubjectData(getSubject());
    data.subjectMap.put(SubjectFields.COMMON_NAME, commonName);
    data.subjectMap.put(SubjectFields.FULL_NAME, commonName + "." + getSuffix());
    return data;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("PrincipalSubjectData [");
    for (Entry<SubjectFields, String> entries : subjectMap.entrySet()) {
      sb.append(entries.getKey()).append("=");
      sb.append(entries.getValue()).append(" ");
    }
    sb.append(']');
    return sb.toString();
  }
}
