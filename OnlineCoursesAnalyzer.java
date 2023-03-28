import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is just a demo for you, please run it on JDK17. This is just a demo, and you can extend and
 * implement functions based on this demo, or implement it in a different way.
 */
public class OnlineCoursesAnalyzer {

  List<Course> courses = new ArrayList<>();

  /**
   * read courses.
   *
   * @param datasetPath the dataset path
   */
  public OnlineCoursesAnalyzer(String datasetPath) {
    BufferedReader br = null;
    String line;
    try {
      br = new BufferedReader(new FileReader(datasetPath, StandardCharsets.UTF_8));
      br.readLine();
      while ((line = br.readLine()) != null) {
        String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
        Course course = new Course(info[0], info[1], new Date(info[2]), info[3], info[4],
            info[5],
            Integer.parseInt(info[6]), Integer.parseInt(info[7]), Integer.parseInt(info[8]),
            Integer.parseInt(info[9]), Integer.parseInt(info[10]),
            Double.parseDouble(info[11]),
            Double.parseDouble(info[12]), Double.parseDouble(info[13]),
            Double.parseDouble(info[14]),
            Double.parseDouble(info[15]), Double.parseDouble(info[16]),
            Double.parseDouble(info[17]),
            Double.parseDouble(info[18]), Double.parseDouble(info[19]),
            Double.parseDouble(info[20]),
            Double.parseDouble(info[21]), Double.parseDouble(info[22]));
        courses.add(course);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Participants count by Institution.
   *
   * @return (institution, count) map
   */
  public Map<String, Integer> getPtcpCountByInst() {
    return courses.stream()
        .sorted((Comparator.comparing(o -> o.institution)))
        .collect(Collectors.groupingBy(o -> o.institution, LinkedHashMap::new,
            Collectors.summingInt(o -> o.participants)));
  }

  /**
   * Participants count by Institution and Course Subject.
   *
   * @return (institution-course Subject, count) map
   */
  public Map<String, Integer> getPtcpCountByInstAndSubject() {
    Map<String, Integer> group = courses.stream()
        .collect(Collectors.groupingBy(o -> o.institution + "-" + o.subject,
            Collectors.summingInt(o -> o.participants)));
    return group.entrySet().stream()
        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldV, newV) -> oldV,
            LinkedHashMap::new));
  }

  /**
   * Course list by Instructor.
   *
   * @return (Instructor, [[course1, course2,...],[coursek,coursek+1,...]]) map
   */
  public Map<String, List<List<String>>> getCourseListOfInstructor() {
    Map<String, List<List<String>>> res = new HashMap<>();
    courses.forEach(c -> {
      String[] instructors = c.instructors.split(",");
      for (String instructor : instructors) {
        instructor = instructor.strip();
        if (!res.containsKey(instructor)) {
          List<List<String>> both = new ArrayList<>();
          both.add(new ArrayList<>());
          both.add(new ArrayList<>());
          res.put(instructor, both);
        }
        if (instructors.length == 1) {
          res.get(instructor).get(0).add(c.title);
        } else {
          res.get(instructor).get(1).add(c.title);
        }
      }
    });
    Map<String, List<List<String>>> ret = new HashMap<>();
    res.forEach((k, v) -> {
      List<String> one = v.get(0).stream()
          .distinct()
          .sorted()
          .toList();
      List<String> two = v.get(1).stream()
          .distinct()
          .sorted()
          .toList();
      List<List<String>> val = new ArrayList<>();
      val.add(one);
      val.add(two);
      ret.put(k, val);
    });
    return ret;
  }

  /**
   * Top courses.
   *
   * @param topK the top K courses
   * @param by the given criterion
   * @return a list of Course titles
   */
  public List<String> getCourses(int topK, String by) {
    if (by.equals("hours")) {
      return courses.stream()
          .sorted(Comparator.comparing(Course::getTotalHours, Comparator.reverseOrder())
              .thenComparing(Course::getTitle))
          .map(Course::getTitle)
          .distinct()
          .limit(topK)
          .collect(Collectors.toList());
    } else {
      return courses.stream()
          .sorted(Comparator.comparing(Course::getParticipants, Comparator.reverseOrder())
              .thenComparing(Course::getTitle))
          .map(Course::getTitle)
          .distinct()
          .limit(topK)
          .collect(Collectors.toList());
    }
  }

  /**
   * Search courses.
   *
   * @param courseSubject Fuzzy matching course subject
   * @param percentAudited the percent of the audited should >= percentAudited
   * @param totalCourseHours the Total Course Hours (Thousands) should <= totalCourseHours
   * @return a list of Course titles
   */
  public List<String> searchCourses(String courseSubject, double percentAudited,
      double totalCourseHours) {
    return courses.stream()
        .filter(o -> o.subject.toUpperCase().contains(courseSubject.toUpperCase())
            && o.percentAudited >= percentAudited && o.totalHours <= totalCourseHours)
        .map(Course::getTitle)
        .distinct()
        .sorted()
        .collect(Collectors.toList());
  }

  /**
   * Recommend courses.
   *
   * @param age age of the user
   * @param gender 0-female, 1-male
   * @param isBachelorOrHigher  0-Not get bachelor degree, 1- Bachelor degree or higher
   * @return a list of Course titles
   */
  public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {
    class Similarity {

      String title;
      double similarity;

      public Similarity(String title, double similarity) {
        this.title = title;
        this.similarity = similarity;
      }

      public String getTitle() {
        return title;
      }

      public void setTitle(String title) {
        this.title = title;
      }

      public double getSimilarity() {
        return similarity;
      }

      public void setSimilarity(double similarity) {
        this.similarity = similarity;
      }
    }

    ArrayList<Similarity> target = new ArrayList<>();
    // get average
    Map<String, Double> number2age = courses.stream().collect(
        Collectors.groupingBy(Course::getNumber,
            Collectors.averagingDouble(Course::getMedianAge)));
    Map<String, Double> number2male = courses.stream().collect(
        Collectors.groupingBy(Course::getNumber,
            Collectors.averagingDouble(Course::getPercentMale)));
    Map<String, Double> number2degree = courses.stream().collect(
        Collectors.groupingBy(Course::getNumber,
            Collectors.averagingDouble(Course::getPercentDegree)));
    // get latest(max) title of course number
    Map<String, Optional<Course>> number2latest = courses.stream()
        .collect(Collectors.groupingBy(Course::getNumber,
            Collectors.maxBy((d1, d2) -> d1.launchDate.compareTo(d2.getLaunchDate()))));

    for (String k : number2age.keySet()) {
      target.add(new Similarity(number2latest.get(k).get().title,
          Math.pow(number2age.get(k) - age, 2) + Math.pow(number2male.get(k) - gender * 100,
              2)
              + Math.pow(number2degree.get(k) - isBachelorOrHigher * 100, 2)));
    }

    return target.stream()
        .sorted(
            Comparator.comparing(Similarity::getSimilarity).thenComparing(Similarity::getTitle))
        .map(o -> o.title)
        .distinct()
        .limit(10)
        .collect(Collectors.toList());
  }

}

class Course {

  String institution;
  String number;
  Date launchDate;
  String title;
  String instructors;
  String subject;
  int year;
  int honorCode;
  int participants;
  int audited;
  int certified;
  double percentAudited;
  double percentCertified;
  double percentCertified50;
  double percentVideo;
  double percentForum;
  double gradeHigherZero;
  double totalHours;
  double medianHoursCertification;
  double medianAge;
  double percentMale;
  double percentFemale;
  double percentDegree;

  public Course(String institution, String number, Date launchDate,
      String title, String instructors, String subject,
      int year, int honorCode, int participants,
      int audited, int certified, double percentAudited,
      double percentCertified, double percentCertified50,
      double percentVideo, double percentForum, double gradeHigherZero,
      double totalHours, double medianHoursCertification,
      double medianAge, double percentMale, double percentFemale,
      double percentDegree) {
    this.institution = institution;
    this.number = number;
    this.launchDate = launchDate;
    if (title.startsWith("\"")) {
      title = title.substring(1);
    }
    if (title.endsWith("\"")) {
      title = title.substring(0, title.length() - 1);
    }
    this.title = title;
    if (instructors.startsWith("\"")) {
      instructors = instructors.substring(1);
    }
    if (instructors.endsWith("\"")) {
      instructors = instructors.substring(0, instructors.length() - 1);
    }
    this.instructors = instructors;
    if (subject.startsWith("\"")) {
      subject = subject.substring(1);
    }
    if (subject.endsWith("\"")) {
      subject = subject.substring(0, subject.length() - 1);
    }
    this.subject = subject;
    this.year = year;
    this.honorCode = honorCode;
    this.participants = participants;
    this.audited = audited;
    this.certified = certified;
    this.percentAudited = percentAudited;
    this.percentCertified = percentCertified;
    this.percentCertified50 = percentCertified50;
    this.percentVideo = percentVideo;
    this.percentForum = percentForum;
    this.gradeHigherZero = gradeHigherZero;
    this.totalHours = totalHours;
    this.medianHoursCertification = medianHoursCertification;
    this.medianAge = medianAge;
    this.percentMale = percentMale;
    this.percentFemale = percentFemale;
    this.percentDegree = percentDegree;
  }

  public String getInstitution() {
    return institution;
  }

  public void setInstitution(String institution) {
    this.institution = institution;
  }

  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  public Date getLaunchDate() {
    return launchDate;
  }

  public void setLaunchDate(Date launchDate) {
    this.launchDate = launchDate;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getInstructors() {
    return instructors;
  }

  public void setInstructors(String instructors) {
    this.instructors = instructors;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public int getYear() {
    return year;
  }

  public void setYear(int year) {
    this.year = year;
  }

  public int getHonorCode() {
    return honorCode;
  }

  public void setHonorCode(int honorCode) {
    this.honorCode = honorCode;
  }

  public int getParticipants() {
    return participants;
  }

  public void setParticipants(int participants) {
    this.participants = participants;
  }

  public int getAudited() {
    return audited;
  }

  public void setAudited(int audited) {
    this.audited = audited;
  }

  public int getCertified() {
    return certified;
  }

  public void setCertified(int certified) {
    this.certified = certified;
  }

  public double getPercentAudited() {
    return percentAudited;
  }

  public void setPercentAudited(double percentAudited) {
    this.percentAudited = percentAudited;
  }

  public double getPercentCertified() {
    return percentCertified;
  }

  public void setPercentCertified(double percentCertified) {
    this.percentCertified = percentCertified;
  }

  public double getPercentCertified50() {
    return percentCertified50;
  }

  public void setPercentCertified50(double percentCertified50) {
    this.percentCertified50 = percentCertified50;
  }

  public double getPercentVideo() {
    return percentVideo;
  }

  public void setPercentVideo(double percentVideo) {
    this.percentVideo = percentVideo;
  }

  public double getPercentForum() {
    return percentForum;
  }

  public void setPercentForum(double percentForum) {
    this.percentForum = percentForum;
  }

  public double getGradeHigherZero() {
    return gradeHigherZero;
  }

  public void setGradeHigherZero(double gradeHigherZero) {
    this.gradeHigherZero = gradeHigherZero;
  }

  public double getTotalHours() {
    return totalHours;
  }

  public void setTotalHours(double totalHours) {
    this.totalHours = totalHours;
  }

  public double getMedianHoursCertification() {
    return medianHoursCertification;
  }

  public void setMedianHoursCertification(double medianHoursCertification) {
    this.medianHoursCertification = medianHoursCertification;
  }

  public double getMedianAge() {
    return medianAge;
  }

  public void setMedianAge(double medianAge) {
    this.medianAge = medianAge;
  }

  public double getPercentMale() {
    return percentMale;
  }

  public void setPercentMale(double percentMale) {
    this.percentMale = percentMale;
  }

  public double getPercentFemale() {
    return percentFemale;
  }

  public void setPercentFemale(double percentFemale) {
    this.percentFemale = percentFemale;
  }

  public double getPercentDegree() {
    return percentDegree;
  }

  public void setPercentDegree(double percentDegree) {
    this.percentDegree = percentDegree;
  }
}
