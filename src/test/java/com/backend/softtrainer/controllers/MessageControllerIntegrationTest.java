package com.backend.softtrainer.controllers;

import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.services.InputMessageService;
import com.backend.softtrainer.services.MessageService;
import com.backend.softtrainer.services.UserMessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MessageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MessageService messageService;

    @Autowired
    private InputMessageService inputMessageService;

    @Autowired
    private UserMessageService userMessageService;

    private String flow = "{\n" +
      "  \"skill\": {\n" +
      "    \"name\": \"skill\"\n" +
      "  },\n" +
      "  \"name\": \"Сем та його безвідповідальність\",\n" +
      "  \"characters\": [\n" +
      "    {\n" +
      "      \"id\": 1,\n" +
      "      \"name\": \"АІ кординатор\",\n" +
      "      \"avatar\": \"https://softtrainer-content.s3.eu-north-1.amazonaws.com/content/memoji.png\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"id\": 2,\n" +
      "      \"name\": \"Сем\",\n" +
      "      \"avatar\": \"https://softtrainer-content.s3.eu-north-1.amazonaws.com/content/56+11.png\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"id\": -1,\n" +
      "      \"name\": \"User\",\n" +
      "      \"avatar\": \"https://softtrainer-content.s3.eu-north-1.amazonaws.com/content/user.png\"\n" +
      "    }\n" +
      "  ],\n" +
      "  \"hyperparameters\": [\n" +
      "    {\n" +
      "      \"key\": \"Involvement\"\n" +
      "    }\n" +
      "  ],\n" +
      "  \"flow\": [\n" +
      "    {\n" +
      "      \"message_id\": 1,\n" +
      "      \"previous_message_id\": [],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"Чудово, ти готовий провести свій перший діалог і навчитись алгоритму якісного зворотного зв'язку. Не бійся імпровізувати, цей алгоритм - лише підказка, основне - це діалог і взаємоповага.\",\n" +
      "      \"character_id\": 1,\n" +
      "      \"show_predicate\": \"\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 2,\n" +
      "      \"previous_message_id\": [1],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"З чого почнемо?\",\n" +
      "      \"character_id\": 1,\n" +
      "      \"show_predicate\": \"\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 3,\n" +
      "      \"previous_message_id\": [2],\n" +
      "      \"message_type\": \"SingleChoiceQuestion\",\n" +
      "      \"text\": \"\",\n" +
      "      \"character_id\": -1,\n" +
      "      \"show_predicate\": \"\",\n" +
      "      \"options\": [\n" +
      "        \"Продумаємо цілі ОС, чого хочемо досягти в результаті діалогу.\",\n" +
      "        \"Головне - почати, з цілями розберемось у процесі.\"\n" +
      "      ],\n" +
      "      \"correct_answer_position\": 1\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 4,\n" +
      "      \"previous_message_id\": [3],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"Це хороша стратегія! Що важливо зробити при підготовці до діалогу?\",\n" +
      "      \"character_id\": 1,\n" +
      "      \"show_predicate\": \"message whereId \\\"3\\\" and message.allCorrect[] and saveChatValue[\\\"Involvement\\\", readChatValue[\\\"Involvement\\\"] + 1.0]\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 5,\n" +
      "      \"previous_message_id\": [3],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"Хм. Давай згадаємо підказку тренера, бо Сем важливий гравець для команди. Він пропонував перед тим як давати ОС, продумати декілька моментів.\",\n" +
      "      \"character_id\": 1,\n" +
      "      \"show_predicate\": \"message whereId \\\"3\\\" and message.allCorrect[].not[] and saveChatValue[\\\"Involvement\\\", readChatValue[\\\"Involvement\\\"] + 1.0]\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 6,\n" +
      "      \"previous_message_id\": [4, 5],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"Що саме ми маємо зрозуміти, щоб зворотній зв'язок краще спрацював?\",\n" +
      "      \"character_id\": 1,\n" +
      "      \"show_predicate\": \"\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 7,\n" +
      "      \"previous_message_id\": [6],\n" +
      "      \"message_type\": \"MultiChoiceTask\",\n" +
      "      \"text\": \"\",\n" +
      "      \"character_id\": -1,\n" +
      "      \"show_predicate\": \"\",\n" +
      "      \"options\": [\n" +
      "        \"Придумати ціль зворотного зв'язку.\",\n" +
      "        \"Зібрати факти й дані, які обґрунтують твій запит до Сема.\",\n" +
      "        \"Продумати питання, які треба поставити Сему.\",\n" +
      "        \"Розпитати всю команду, що вони думають про Сема.\"\n" +
      "      ],\n" +
      "      \"correct_answer_positions\": [1, 2, 3]\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 8,\n" +
      "      \"previous_message_id\": [7],\n" +
      "      \"prompt\": \"\",\n" +
      "      \"message_type\": \"HintMessage\",\n" +
      "      \"character_id\": 1,\n" +
      "      \"show_predicate\": \"\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 9,\n" +
      "      \"previous_message_id\": [8],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"Точно в ціль! Має спрацювати.\",\n" +
      "      \"character_id\": 1,\n" +
      "      \"show_predicate\": \"message whereId \\\"7\\\" and message.allCorrect[]\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 10,\n" +
      "      \"previous_message_id\": [8],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"Думаю основне таки продумати ціль, продумати питання щоб зрозуміти ситуацію Сема, та зібрати факти.\",\n" +
      "      \"character_id\": 1,\n" +
      "      \"show_predicate\": \"message whereId \\\"7\\\" and message.allCorrect[].not[]\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 11,\n" +
      "      \"previous_message_id\": [9, 10],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"Як ти бачиш мету діалогу?\",\n" +
      "      \"character_id\": 1,\n" +
      "      \"show_predicate\": \"\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 12,\n" +
      "      \"previous_message_id\": [11],\n" +
      "      \"message_type\": \"SingleChoiceQuestion\",\n" +
      "      \"text\": \"\",\n" +
      "      \"character_id\": -1,\n" +
      "      \"show_predicate\": \"\",\n" +
      "      \"options\": [\n" +
      "        \"Сем відновив подачу всіх звітів й участь у кожній командній зустрічі!\",\n" +
      "        \"Сем почав сприймати вас як керівника і завжди робив те, що ви просите від нього без дорікань.\",\n" +
      "        \"Покращення координації по проєкту. Для цього важливо, щоб Сем був присутнім на щотижневих зустрічах та надсилав звітність у строк.\"\n" +
      "      ],\n" +
      "      \"correct_answer_position\": 3\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 13,\n" +
      "      \"previous_message_id\": [12],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"Супер! Тепер ти точно знаєш, який результат хочеш отримати. Це зробить діалог у рази продуктивніше. Час починати. Будь уважним кожна наступна відповідь безпосередньо буде впливати на результат та залученість Сема!\",\n" +
      "      \"character_id\": 1,\n" +
      "      \"show_predicate\": \"message whereId \\\"12\\\" and message.allCorrect[]\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 14,\n" +
      "      \"previous_message_id\": [12],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"Розумію вашу ціль! Згадуючи підказку тренера я б порадив ще обов'язково додати як це вплине на загальний результат проєкту!\",\n" +
      "      \"character_id\": 1,\n" +
      "      \"show_predicate\": \"message whereId \\\"12\\\" and message.allCorrect[].not[]\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 15,\n" +
      "      \"previous_message_id\": [13, 14],\n" +
      "      \"message_type\": \"SingleChoiceQuestion\",\n" +
      "      \"text\": \"\",\n" +
      "      \"character_id\": -1,\n" +
      "      \"show_predicate\": \"\",\n" +
      "      \"options\": [\n" +
      "        \"Привіт, Cем! Хочу обговорити декілька організаційних питань стосовно зустрічей, це займе близько 10-15 хв. Можемо зараз?\",\n" +
      "        \"Привіт. Треба поговорити. Зайди до мене будь ласка коли зможеш. Це серйозно.\",\n" +
      "        \"Привіт! Я помітив/ла, що після звільнення попереднього керівника ти перестав готувати та відправляти щотижневі звіти, а також часто пропускаєш командні зустрічі. Будь ласка, зверни на це увагу.\"\n" +
      "      ],\n" +
      "      \"correct_answer_position\": 1\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 16,\n" +
      "      \"previous_message_id\": [15],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"Привіт! Прямо зараз не дуже зручно це обговорювати! Маю закінчити одну справу. Давай за 15 хвилин поговоримо?\",\n" +
      "      \"character_id\": 2,\n" +
      "      \"show_predicate\": \"message whereId \\\"15\\\" and (message.selected[] == [1] or message.selected[] == [3])\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 17,\n" +
      "      \"previous_message_id\": [16],\n" +
      "      \"message_type\": \"SingleChoiceQuestion\",\n" +
      "      \"text\": \"\",\n" +
      "      \"character_id\": -1,\n" +
      "      \"show_predicate\": \"\",\n" +
      "      \"options\": [\n" +
      "        \"Добре, зустрінемось за 15 хв!\",\n" +
      "        \"Ні, давай краще прямо зараз бо далі у мене багато роботи!\",\n" +
      "        \"Хм. (Показати невдоволення, але нічого не сказати)\"\n" +
      "      ],\n" +
      "      \"correct_answer_position\": 1\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 18,\n" +
      "      \"previous_message_id\": [17],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"(повертаємось через 15 хв.)\",\n" +
      "      \"character_id\": 1,\n" +
      "      \"show_predicate\": \"message whereId \\\"17\\\" and (message.selected[] == [1] or message.selected[] == [3])\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 19,\n" +
      "      \"previous_message_id\": [15],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"Привіт! Хм, чесно кажучи я трохи стурбований, щось не так?\",\n" +
      "      \"character_id\": 2,\n" +
      "      \"show_predicate\": \"message whereId \\\"15\\\" and message.selected[] == [2]\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 20,\n" +
      "      \"previous_message_id\": [18, 19],\n" +
      "      \"message_type\": \"SingleChoiceQuestion\",\n" +
      "      \"text\": \"\",\n" +
      "      \"character_id\": -1,\n" +
      "      \"show_predicate\": \"\",\n" +
      "      \"options\": [\n" +
      "        \"Я помітив, що за останній місяць ти регулярно пропускав щотижневі зустрічі і звіти. Був лише на одній за минулий місяць. Вже бачу, що це призводить до розсинхронізації: ми тестуємо по кілька разів одну й ту саму зміну, витрачаючи на це кошти. Для мене важливо зараз відновити зустрічі та звіти, адже це зможе давати всім регулярно розуміння про реальну ситуацію, не повторювати попередні помилки та ефективніше планувати. Що ти думаєш про це?\",\n" +
      "        \"Я не задоволений/а, що ти перестав відвідувати командні зустрічі та подавати звіти. З наступного тижня прошу відновити це, адже це впливає на нашу ефективність. Прошу надсилати мені особисто.\"\n" +
      "      ],\n" +
      "      \"correct_answer_position\": 1\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 21,\n" +
      "      \"previous_message_id\": [20],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"Яку зміну ми тестували кілька разів?\",\n" +
      "      \"character_id\": 2,\n" +
      "      \"show_predicate\": \"\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 22,\n" +
      "      \"previous_message_id\": [21],\n" +
      "      \"message_type\": \"SingleChoiceQuestion\",\n" +
      "      \"text\": \"\",\n" +
      "      \"character_id\": -1,\n" +
      "      \"show_predicate\": \"\",\n" +
      "      \"options\": [\n" +
      "        \"Ти навіть не знаєш, яку ми зміну тестували?\",\n" +
      "        \"Спитай у Олега, будь ласка.\",\n" +
      "        \"Ми змінили розташування кнопки і відправили це в A/B тест учора, хоча на командній зустрічі 2 тижні тому Олег уже казав, що проводив це тестування самостійно й це не принесло бажаний результат.\"\n" +
      "      ],\n" +
      "      \"correct_answer_position\": 2\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 23,\n" +
      "      \"previous_message_id\": [22],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"Хм, добре, зупинимо цей тест.\",\n" +
      "      \"character_id\": 2,\n" +
      "      \"show_predicate\": \"\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 24,\n" +
      "      \"previous_message_id\": [23],\n" +
      "      \"message_type\": \"SingleChoiceQuestion\",\n" +
      "      \"text\": \"\",\n" +
      "      \"character_id\": -1,\n" +
      "      \"show_predicate\": \"\",\n" +
      "      \"options\": [\n" +
      "        \"Дякую, підкажи є якісь причини, чому ти перестав відвідувати зустрічі та подавати звіти?\",\n" +
      "        \"Як ти думаєш, чому набагато менше людей почало відвідувати зустрічі і що ми можемо зробити, щоб ситуація стала краще?\",\n" +
      "        \"Призупиніть і почніть, будь ласка, ходити на зустрічі, щоб такого більше не траплялось. Дякую.\"\n" +
      "      ],\n" +
      "      \"correct_answer_position\": 2\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 25,\n" +
      "      \"previous_message_id\": [24],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"Я завжди вважав, що ці зустрічі більше для керівництва, ніж для команди. Після того, як Сергій, наш минулий тім лід, пішов, я подумав, що команда може спілкуватися та вирішувати питання і без цих формальних мітів та щотижневих звітів. Бо це займає декілька годин! А дедлайни горять!\",\n" +
      "      \"character_id\": 2,\n" +
      "      \"show_predicate\": \"\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 26,\n" +
      "      \"previous_message_id\": [25],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"Мені здається, нам не потрібно так багато часу на звіти та зустрічі. У нас в команді відповідальні фахівці, і в 99% випадків ми встигаємо все до дедлайну. Якщо необхідно, я можу особисто уточнити деталі. Я говорив з кількома членами команди, і вони не проти скасувати цей звіт.\",\n" +
      "      \"character_id\": 2,\n" +
      "      \"show_predicate\": \"\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 27,\n" +
      "      \"previous_message_id\": [26],\n" +
      "      \"message_type\": \"SingleChoiceQuestion\",\n" +
      "      \"text\": \"\",\n" +
      "      \"character_id\": -1,\n" +
      "      \"show_predicate\": \"\",\n" +
      "      \"options\": [\n" +
      "        \"Це хто хоче скасувати? Я з ними поговорю. Для нас це критично!\",\n" +
      "        \"Давай так, для мене як для ного керівника це потрібно! Тому будь ласка не пропускайте.\",\n" +
      "        \"Ми можемо розглянути можливості зробити ці зустрічі більш продуктивними та короткими для всіх. Можливо, ти вважаєш, що можна якось поліпшити цей формат? Я буду вдячний за твої ідеї.\"\n" +
      "      ],\n" +
      "      \"correct_answer_position\": 3\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 28,\n" +
      "      \"previous_message_id\": [27],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"Я насправді не думав, що це має такий вплив і важливо для тебе. Щодо формату, подумав якщо зробімо щотижневі звіти в чаті, щоб ми бачили прогрес один одного, а командні зустрічі проводитемо не в понеділок, адже зазвичай у цей день навалюється багато роботи.\",\n" +
      "      \"character_id\": 2,\n" +
      "      \"show_predicate\": \"\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 29,\n" +
      "      \"previous_message_id\": [28],\n" +
      "      \"message_type\": \"SingleChoiceQuestion\",\n" +
      "      \"text\": \"\",\n" +
      "      \"character_id\": -1,\n" +
      "      \"show_predicate\": \"\",\n" +
      "      \"options\": [\n" +
      "        \"Супер, тоді домовимось, що будемо ділитись звітами в окремому загальному чаті щотижня та перенесемо звіт на середу. Я можу чимось допомогти для цього?\",\n" +
      "        \"Так, будь ласка. На жаль, надалі за кожен не наданий звіт буде накладатись штраф. Тому сподіваюсь на розуміння.\"\n" +
      "      ],\n" +
      "      \"correct_answer_position\": 1\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 30,\n" +
      "      \"previous_message_id\": [29],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"Зможеш написати про це в спільному чаті? Це було б круто і показало б команді, що будемо працювати в новому режимі. Дякую, що почув, Сергій ніколи особливо не прислухався до порад, тому іноді важко було працювати)\",\n" +
      "      \"character_id\": 2,\n" +
      "      \"show_predicate\": \"message whereId \\\"29\\\" and message.allCorrect[]\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 31,\n" +
      "      \"previous_message_id\": [29],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"Ох, я зрозумів, що для тебе це критично важливо, але думаю це трохи налякає команду, хоча дивись сам.\",\n" +
      "      \"character_id\": 2,\n" +
      "      \"show_predicate\": \"message whereId \\\"29\\\" and message.allCorrect[].not[]\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 32,\n" +
      "      \"previous_message_id\": [30, 31],\n" +
      "      \"message_type\": \"Text\",\n" +
      "      \"text\": \"Cупер, дякую тобі!\",\n" +
      "      \"character_id\": -1,\n" +
      "      \"show_predicate\": \"\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"message_id\": 33,\n" +
      "      \"previous_message_id\": [32],\n" +
      "      \"prompt\": \"\",\n" +
      "      \"message_type\": \"ResultSimulation\",\n" +
      "      \"character_id\": 1,\n" +
      "      \"show_predicate\": \"\"\n" +
      "    }\n" +
      "  ]\n" +
      "}\n";

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    public void testHintMessageIsReadyToBeRead () throws Exception {

        //init db with flow
        mockMvc.perform(put("/flow/upload")
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(flow))
          .andExpect(status().isOk());

//        MessageRequestDto messageRequestDto = new MessageRequestDto();
//        messageRequestDto.setChatId(chatRepository.findAll().get(0).getId());
//        messageRequestDto.setId(messageRepository.findAll().get(0).getId());
//
//        mockMvc.perform(put("/message/send")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(Converter.toJson(messageRequestDto)))
//                .andExpect(status().isOk());
    }

}
