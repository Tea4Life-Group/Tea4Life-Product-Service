package tea4life.product_service.advice.exception;

/**
 * @author Le Tran Gia Huy
 * @created 09/04/2026 - 6:08 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.advice.exception
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
