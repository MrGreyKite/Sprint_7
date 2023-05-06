package ru.bagmet;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.*;
import ru.bagmet.data.CourierCredentials;
import ru.bagmet.data.OrderData;

import java.util.Arrays;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

@Tag("order")
@DisplayName("Тесты на получение списка заказов")
public class OrdersListTest {

    static CourierClient courierClient = new CourierClient();
    static OrderClient orderClient = new OrderClient();
    CourierCredentials courier;
    int courierId;
    OrderData order1 = new OrderData("Имя1", "Фамилия1", "Адрес1", "1", "Телефон1", 1, "5.05", "!");
    OrderData order2 = new OrderData("Имя2", "Фамилия2", "Адрес2", "2", "Телефон2", 2, "10.05", "!");
    int orderID1;
    int orderID2;

    @Test
    @DisplayName("Получение списка всех существующих заказов")
    @Description("Проверка, что при запросе всех существующих заказов каждый заказ содержит основные обязательные поля")
    public void getAllOrders() {
        ValidatableResponse response = orderClient.getOrders();
        OrderData[] orders = response.extract().jsonPath().getObject("orders", OrderData[].class);

        Allure.step("Проверка корректности данных в ответе: у каждого заказа есть id и трек-номер", () -> {
            for (OrderData order : orders) {
                assertAll("Заказ и трек-номер",
                        () -> assertNotEquals(0, order.getTrack()),
                        () -> assertNotEquals(0, order.getId())
                );
            }

            //альтернативный способ проверки для иллюстрации возможностей встроенных ассертов restAssured
            response.assertThat().body("orders.track", everyItem(notNullValue()));
            response.assertThat().body("orders.id", everyItem(notNullValue()));
        });

    }

    @Test
    @DisplayName("Получение списка всех заказов конкретного курьера")
    @Description("Проверка, что при запросе заказов с указанием ID курьера, приходят только данные этого курьера")
    public void getAllOrdersForSomeCourier(){

        Allure.step("Подготовка тестовых данных: курьер и заказы в разных статусах", () -> {
            courier = new CourierCredentials("logo-test", "pass", "name");
            courierClient.createCourier(courier);
            courierId = courierClient.loginWithCourier(courier).extract().body().path("id");

            int trackNumber1 = orderClient.createOrder(order1).extract().path("track");
            int trackNumber2 = orderClient.createOrder(order2).extract().path("track");

            orderID1 = orderClient.getOrderByTrack(trackNumber1).extract().path("order.id");
            orderID2 = orderClient.getOrderByTrack(trackNumber2).extract().path("order.id");

        });
        orderClient.acceptOrder(orderID1, courierId);
        orderClient.acceptOrder(orderID2, courierId);
        orderClient.finishOrder(orderID1);

        ValidatableResponse response = orderClient.getOrders(courierId);

        OrderData[] orders = response.extract().jsonPath().getObject("orders", OrderData[].class);
        System.out.println(Arrays.toString(orders));

        int deliveryStatus = response.extract().jsonPath().
                getInt(String.format("orders.find{it.id == %d}.status", orderID2));
        int finishedStatus = response.extract().jsonPath().
                getInt(String.format("orders.find{it.id == %d}.status", orderID1));

        Allure.step("Проверка корректности данных в ответе", () -> {
            assertAll("Правильный курьер и статусы доставки",
                    () -> {
                        for (OrderData order : orders) {
                            assertEquals(courierId, order.getCourierId());
                            }
                        },
                    () -> assertEquals(1, deliveryStatus, "Статус заказа в работе не соответствует"),
                    () -> assertEquals(2, finishedStatus, "Статус доставленного заказа не соответствует"),
                    () -> assertEquals(2, orders.length, "Количество заказов не соответствует")
            );
        });

    }

    @AfterEach
    @Step("Очистка тестовых данных")
    void tearDown(){
        if(courier != null) {
            courierClient.deleteCourierByID(courierId);
        }
    }

}
