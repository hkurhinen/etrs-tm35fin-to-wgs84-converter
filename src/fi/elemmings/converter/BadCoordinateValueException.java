package fi.elemmings.converter;

public class BadCoordinateValueException extends Exception {

  private static final long serialVersionUID = 1L;

  public BadCoordinateValueException(String msg) {
    super(msg);
  }

}
