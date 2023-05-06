package ru.bagmet;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.*;
import ru.bagmet.data.CourierCredentials;
import ru.bagmet.data.OrderData;
import ru.bagmet.data.StatusCodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static ru.bagmet.data.StatusCodes.*;

@Tag("order")
@DisplayName("Тесты на взятие заказа курьером")
public class OrderAcceptanceTest {
    static CourierClient courierClient = new CourierClient();
    static OrderClient orderClient = new OrderClient();
    static CourierCredentials courier;
    static int courierId;
    int id;
    OrderData order;

    @BeforeAll
    @Step("Создание тестового курьера для взятия заказов")
    static void setUp() {
        courier = new CourierCredentials("courier" + new Random().nextInt(100), "password",
                "CourierName" + new Random().nextInt(100));
        courierClient.createCourier(courier);
        courierId = courierClient.loginWithCourier(courier).extract().path("id");
    }

    @Test
    @DisplayName("Принятие существующего заказа")
    @Description("Проверка, что можно принять существующий активный заказ, который еще не взят ни одним курьером")
    public void acceptAnExistingOrder() {
        Allure.step("Подготовка тестовых данных - ID случайноо заказа, который еще никто не брал",  () -> {
                    //найти все заказы, где не заполнено id курьера
                    List<OrderData> orders = orderClient.getOrders().extract().
                            jsonPath().getList("orders.findAll { it.courierId == null }", OrderData.class);

                    //если заказов с пустым courierId нет, то создаем новый тестовый заказ
                    if (orders.isEmpty()) {
                        order = new OrderData("Имя", "Фамилия", "Адрес такой-то",
                                "5", "+79099099999", 2, "2023-06-06", "Тестируем");
                        int id = orderClient.getOrderByTrack(orderClient.createOrder(order).
                                extract().path("track")).extract().path("order.id");
                        order.setId(id);
                    } else {
                        int randomIndex = new Random().nextInt(orders.size());
                        order = orders.get(randomIndex);
                    }
        });

        ValidatableResponse response = orderClient.acceptOrder(order.getId(), courierId);

        Allure.step("Проверка корректности данных в ответе", () -> {
            assertAll("Приходит правильный статус-код и подтверждается взятие заказа",
                    () -> assertEquals(OK.getCode(), response.extract().statusCode()),
                    () -> assertThat(response.extract().path("ok"), is(true))
            );
        });

        ValidatableResponse resp = orderClient.getOrderByTrack(order.getTrack());
        OrderData ourOrder = resp.extract().jsonPath().getObject("order", OrderData.class);

        Allure.step("Проверка изменения данных в самом заказе", () -> {
            assertAll("Приходит корректная информация об изменившихся данных в заказе",
                    () -> assertNotNull(ourOrder.getCourierFirstName()),
                    () -> assertThat(ourOrder.isInDelivery(), is(true)),
                    () -> assertEquals(1, (Integer) resp.extract().path("order.status"))
            );
        });

    }

    @Test
    @DisplayName("Принятие несуществующего заказа")
    @Description("Проверка сообщения об ошибке при попытке взять заказ, которого нет")
    public void acceptNonExistingOrder() {
        Allure.step("Подготовка тестовых данных - ID, которого нет в списке активных заказов",  () -> {
            ValidatableResponse r = orderClient.getOrders();
            OrderData[] orders = r.extract().jsonPath().getObject("orders", OrderData[].class);
            int total = r.extract().path("pageInfo.total");

            List<Integer> ids = new ArrayList<>();
            for (OrderData order : orders) {
                ids.add(order.getId());
            }

            int maxNumber = Collections.max(ids);
            id = total + maxNumber;
        });

        ValidatableResponse response = orderClient.acceptOrder(id, courierId);

        Allure.step("Проверка корректности данных в ответе: сообщение об ошибке", () -> {
            assertAll("Приходит правильный статус-код и сообщение об ошибке",
                    () -> assertEquals(NOT_FOUND.getCode(), response.extract().statusCode()),
                    () -> assertEquals("Заказа с таким id не существует",
                            response.extract().body().path("message"))
            );
        });
    }

    @Test
    @DisplayName("Принятие заказа несуществующим курьером")
    @Description("Проверка сообщения об ошибке при попытке взять заказ курьером, которого нет")
    public void acceptOrderWithInvalidCourier() {
        Allure.step("Подготовка тестовых данных - ID случайного заказа, который еще никто не брал",  () -> {
                    List<OrderData> orders = orderClient.getOrders().extract().
                            jsonPath().getList("orders.findAll { it.courierId == null }", OrderData.class);

                    if (orders.isEmpty()) {
                        order = new OrderData("Имя", "Фамилия", "Адрес такой-то",
                                "5", "+79099099990", 2, "2023-06-06", "Т");
                        int id = orderClient.getOrderByTrack(orderClient.createOrder(order).
                                extract().path("track")).extract().path("order.id");
                        order.setId(id);
                    } else {
                        int randomIndex = new Random().nextInt(orders.size());
                        order = orders.get(randomIndex);
                    }
        });

        ValidatableResponse response = orderClient.acceptOrder(order.getId(), new Random().nextInt(1000000));

        Allure.step("Проверка корректности данных в ответе: сообщение об ошибке", () -> {
            assertAll("Приходит правильный статус-код и сообщение об ошибке",
                    () -> assertEquals(NOT_FOUND.getCode(), response.extract().statusCode()),
                    () -> assertEquals("Курьера с таким id не существует",
                            response.extract().body().path("message"))
            );
        });

    }

    @Test
    @DisplayName("Принятие заказа, который уже был в работе")
    @Description("Проверка наличия ошибки при попытке взять в работу уже однажды взятый заказ")
    public void acceptOrderAlreadyInDelivery() {
        order = new OrderData("Имя", "Фамилия", "Адрес такой-то",
                    "5", "+79099099999", 2, "2023-06-06", "Тестируем");
        int id = orderClient.getOrderByTrack(orderClient.createOrder(order).
                    extract().path("track")).extract().path("order.id");
        order.setId(id);

        orderClient.acceptOrder(order.getId(), courierId);
        ValidatableResponse responseRepeat = orderClient.acceptOrder(order.getId(), courierId);

        Allure.step("Проверка корректности данных в ответе: сообщение об ошибке", () -> {
            assertAll("Приходит правильный статус-код и сообщение об ошибке",
                    () -> assertEquals(CONFLICT.getCode(), responseRepeat.extract().statusCode()),
                    () -> assertEquals("Этот заказ уже в работе",
                            responseRepeat.extract().body().path("message"))
            );
        });
    }

    @Test
    @DisplayName("Принятие заказа без id курьера")
    public void acceptOrderWithoutCourierID() {
        Allure.step("Подготовка тестовых данных - ID случайного заказа, который еще никто не брал",  () -> {
            List<OrderData> orders = orderClient.getOrders().extract().
                    jsonPath().getList("orders.findAll { it.courierId == null }", OrderData.class);
                int randomIndex = new Random().nextInt(orders.size());
                order = orders.get(randomIndex);
        });
        ValidatableResponse response = orderClient.acceptOrder(order.getId(), 0);

        Allure.step("Проверка корректности данных в ответе: сообщение об ошибке", () -> {
            assertAll("Приходит правильный статус-код и сообщение об ошибке",
                    () -> assertEquals(BAD_REQUEST.getCode(), response.extract().statusCode()),
                    () -> assertEquals("Недостаточно данных для поиска",
                            response.extract().body().path("message"))
            );
        });
    }

    @Test
    @DisplayName("Принятие заказа без id заказа")
    public void acceptOrderWithoutOrderID() {
        ValidatableResponse response = orderClient.acceptOrder(0, new Random().nextInt(1000000));

        Allure.step("Проверка корректности данных в ответе: сообщение об ошибке", () -> {
            assertAll("Приходит правильный статус-код и сообщение об ошибке",
                    () -> assertEquals(BAD_REQUEST.getCode(), response.extract().statusCode()),
                    () -> assertEquals("Недостаточно данных для поиска",
                            response.extract().body().path("message"))
            );
        });

    }

    @AfterAll
    @Step("Очистка тестовых данных")
    static void tearDown(){
        if(courier != null) {
            courierClient.deleteCourierByID(courierId);
        }
    }
}
