package ru.bagmet;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.bagmet.data.OrderData;
import ru.bagmet.data.StatusCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("order")
@DisplayName("Тесты на получение заказа по номеру")
public class OrderGetTest {
    static OrderClient orderClient = new OrderClient();
    int trackNumber;

    @Test
    @DisplayName("Получение заказа по валидному трек-номеру")
    @Description("Ищется случайный заказ в списке всех заказов и проверяется наличие у него необходимых полей")
    public void getOrderByValidTrackNumber() {

        Allure.step("Поиск случайного заказа из всех существующих", () -> {
                    OrderData[] orders = orderClient.getOrders().
                            extract().jsonPath().getObject("orders", OrderData[].class);
                    int rnd = new Random().nextInt(orders.length);
                    trackNumber = orders[rnd].getTrack();
                });

        ValidatableResponse response = orderClient.getOrderByTrack(trackNumber).spec(orderClient.getResponseSpecForOK());

        Allure.step("Проверка корректности данных: наличие полей", () -> {
            response.assertThat()
                    .body("order", hasKey("id"))
                    .body("order", hasKey("firstName"))
                    .body("order", hasKey("lastName"))
                    .body("order", hasKey("address"))
                    .body("order", hasKey("metroStation"))
                    .body("order", hasKey("phone"))
                    .body("order", hasKey("rentTime"))
                    .body("order", hasKey("deliveryDate"))
                    .body("order", hasKey("track"))
                    .body("order", hasKey("status"))
                    .body("order", hasKey("comment"))
                    .body("order", hasKey("createdAt"));
        });
    }

    @Test
    @DisplayName("Получение заказа по невалидному трек-номеру")
    @Description("Ищется заказ с несуществующим трек-номером и проверяется сообщение об ошибке")
    public void getOrderByInvalidTrackNumber() {
        OrderData[] orders = orderClient.getOrders().extract().jsonPath().getObject("orders", OrderData[].class);

        Allure.step("Подготовка тестовых данных - трек-номер, которого нет в списке",  () -> {
            List<Integer> trackNumbers = new ArrayList<>();
            for (OrderData order : orders) {
                trackNumbers.add(order.getTrack());
            }

            do {
                trackNumber = new Random().nextInt(1000000);
            } while (trackNumbers.contains(trackNumber)); // проверяем, есть ли число в списке всех трек-номеров
        });

        ValidatableResponse response = orderClient.getOrderByTrack(trackNumber);

        Allure.step("Проверка корректности данных в ответе: сообщение об ошибке", () -> {
            assertAll("Приходит правильный статус-код и сообщение об ошибке",
                    () -> assertEquals(StatusCodes.NOT_FOUND.getCode(), response.extract().statusCode()),
                    () -> assertEquals("Заказ не найден",
                            response.extract().body().path("message"))
            );
        });

    }

    @Test
    @DisplayName("Отправка запроса на получение заказа без трек-номера")
    @Description("Проверяется корректность сообщения об ошибке при отсутствующем трек-номере")
    public void getOrderWithoutTrackNumber() {
        ValidatableResponse response = orderClient.getOrderByTrack(trackNumber);

        Allure.step("Проверка корректности данных в ответе: сообщение об ошибке", () -> {
            assertAll("Приходит правильный статус-код и сообщение об ошибке",
                    () -> assertEquals(StatusCodes.BAD_REQUEST.getCode(), response.extract().statusCode()),
                    () -> assertEquals("Недостаточно данных для поиска",
                            response.extract().body().path("message"))
            );
        });
    }

}
