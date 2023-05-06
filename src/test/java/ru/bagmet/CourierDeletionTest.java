package ru.bagmet;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.bagmet.data.CourierCredentials;
import ru.bagmet.data.StatusCodes;

import java.util.Random;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static ru.bagmet.data.StatusCodes.*;

@Tag("courier")
@DisplayName("Тесты на удаление курьера")
public class CourierDeletionTest {
    static CourierClient courierClient = new CourierClient();
    Random random = new Random();
    String login = "BrandNewLogin" + random.nextInt(100);
    String password = "secret" + random.nextInt(1000);
    String firstName = "Курьер";
    int id;

    @Test
    @DisplayName("Удаление действительно существующего курьера")
    @Description("Проверяется возможность удалить ранее созданного курьера")
    public void deleteExistingCourier() {
        CourierCredentials courier = new CourierCredentials(login, password, firstName);
        courierClient.createCourier(courier);
        id = courierClient.loginWithCourier(courier).extract().body().path("id");

        ValidatableResponse response = courierClient.deleteCourierByID(id);

        Allure.step("Проверка корректности данных в ответе на запрос удаления", () -> {
            assertAll("Приходит правильный статус-код и подтверждается удаление курьера",
                    () -> assertEquals(OK.getCode(), response.extract().statusCode()),
                    () -> assertThat(response.extract().path("ok"), is(true))
            );
        });
    }

    @Test
    @DisplayName("Удаление курьера по несуществующему id")
    @Description("Проверяется наличие ошибки при запросе на удаление несуществующего курьера")
    public void deleteByNonExistingID() {
        id = random.nextInt(10000) + 1000;

        ValidatableResponse response = courierClient.deleteCourierByID(id);

        Allure.step("Проверка корректности данных в ответе на запрос удаления", () -> {
            assertAll("Приходит правильный статус-код и сообщение об ошибке",
                    () -> assertEquals(NOT_FOUND.getCode(), response.extract().statusCode()),
                    () -> assertEquals("Курьера с таким id нет",
                            response.extract().body().path("message"))
            );
        });
    }

    @Test
    @DisplayName("Удаление курьера без указания id")
    @Description("Проверяется корректность сообщения об ошибке при запросе на удаление без id курьера")
    public void deleteWithoutIdParam() {
        ValidatableResponse response = courierClient.deleteCourierByID("");

        Allure.step("Проверка корректности данных в ответе на запрос удаления", () -> {
            assertAll("Приходит правильный статус-код и сообщение об ошибке",
                    () -> assertEquals(BAD_REQUEST.getCode(), response.extract().statusCode()),
                    () -> assertEquals("Недостаточно данных для удаления курьера",
                            response.extract().body().path("message"))
            );
        });
    }

}
