
package br.com.analise.investimentos;

public class DatabricksException extends RuntimeException {

    private final Integer statusHttp;

    public DatabricksException(String message, Throwable cause) {
        super(message, cause);
        this.statusHttp = null;
    }

    public DatabricksException(String message, Integer statusHttp, Throwable cause) {
        super(message, cause);
        this.statusHttp = statusHttp;
    }

    public Integer getStatusHttp() {
        return statusHttp;
    }
}
