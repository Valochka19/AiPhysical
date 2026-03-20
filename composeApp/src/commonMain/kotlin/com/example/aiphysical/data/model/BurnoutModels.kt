package com.example.aiphysical.data.model

import com.example.aiphysical.presentation.auth.AppLanguage
import com.example.aiphysical.presentation.student.StudentTestType
import com.example.aiphysical.presentation.student.displayLabel
import kotlin.math.roundToInt

// ── Answer options (5 variants with weight 0..4) ──────────────────────────────
enum class AnswerType(val label: String, val weight: Int) {
    EXACTLY_ME("Да, это точно про меня", 4),
    SIMILAR("Схоже со мной, но не настолько", 3),
    NEUTRAL("Я нахожусь между таким состоянием", 2),
    PROBABLY_NOT("Скорее всего — нет", 1),
    NOT_ME("Нет, это точно не про меня", 0)
}

fun AnswerType.displayLabel(language: AppLanguage): String = when (language) {
    AppLanguage.RU -> label
    AppLanguage.EN -> when (this) {
        AnswerType.EXACTLY_ME -> "Yes, this is exactly about me"
        AnswerType.SIMILAR -> "Similar to me, but not fully"
        AnswerType.NEUTRAL -> "Somewhere in between"
        AnswerType.PROBABLY_NOT -> "Probably not"
        AnswerType.NOT_ME -> "No, definitely not me"
    }
    AppLanguage.KZ -> when (this) {
        AnswerType.EXACTLY_ME -> "Иә, бұл дәл мен туралы"
        AnswerType.SIMILAR -> "Маған ұқсайды, бірақ толық емес"
        AnswerType.NEUTRAL -> "Екеуінің ортасындамын"
        AnswerType.PROBABLY_NOT -> "Сірә, жоқ"
        AnswerType.NOT_ME -> "Жоқ, бұл мен туралы емес"
    }
}

// ── Cat emotional states ───────────────────────────────────────────────────────
enum class CatState {
    IDLE, HAPPY, NERVOUS, PROUD, TIRED, ENERGETIC,
    STRESS, PEACEFUL, OVERWHELMED, LAUGHING, IN_BOX
}

enum class QuestionPolarity { POSITIVE, NEGATIVE }

enum class MetricSemantics { HIGH_IS_BAD, HIGH_IS_GOOD }

data class StudentTestQuestion(
    val id: Int,
    val text: String,
    val catEmotion: CatState,
    val polarity: QuestionPolarity
)

data class StudentTestAnswer(
    val questionId: Int,
    val questionText: String,
    val catEmotion: CatState,
    val answerType: AnswerType,
    val polarity: QuestionPolarity
) {
    val answerLabel: String get() = answerType.label
    val weight: Int get() = answerType.weight
}

sealed class StudentTestStep {
    object Questions : StudentTestStep()
    object LoadingResult : StudentTestStep()
    data class Result(
        val feedbackText: String,
        val score: Int,
        val aiAssessment: String
    ) : StudentTestStep()
}

data class StudentTestUiState(
    val definition: StudentTestDefinition,
    val step: StudentTestStep = StudentTestStep.Questions,
    val currentQuestionIndex: Int = 0,
    val answers: List<StudentTestAnswer> = emptyList(),
    val isAnswering: Boolean = false,
    val errorMessage: String? = null
)

data class StudentTestDefinition(
    val type: StudentTestType,
    val testId: String,
    val testName: String,
    val profileField: String,
    val semantics: MetricSemantics,
    val purpose: String,
    val focusHint: String,
    val questions: List<StudentTestQuestion>
) {
    init {
        require(questions.size == 10) { "Each student test must have exactly 10 questions: $testId" }
    }

    fun scoreAnswers(answers: List<StudentTestAnswer>): Int {
        if (answers.isEmpty()) return 0
        val distressSum = answers.sumOf { answer ->
            when (answer.polarity) {
                QuestionPolarity.POSITIVE -> 4 - answer.answerType.weight
                QuestionPolarity.NEGATIVE -> answer.answerType.weight
            }
        }
        val distressPercent = distressSum * 100f / (answers.size * 4f)
        val metricScore = when (semantics) {
            MetricSemantics.HIGH_IS_BAD -> distressPercent
            MetricSemantics.HIGH_IS_GOOD -> 100f - distressPercent
        }
        return metricScore.roundToInt().coerceIn(0, 100)
    }

    fun computeAssessment(score: Int): String = when (semantics) {
        MetricSemantics.HIGH_IS_BAD -> when {
            score >= CRITICAL_THRESHOLD -> "critical"
            score >= RISK_STRESS_THRESHOLD -> "stress"
            else -> "normal"
        }

        MetricSemantics.HIGH_IS_GOOD -> when {
            score >= POSITIVE_NORMAL_THRESHOLD -> "normal"
            score >= POSITIVE_STRESS_THRESHOLD -> "stress"
            else -> "critical"
        }
    }

    fun healthContribution(score: Float): Float = when (semantics) {
        MetricSemantics.HIGH_IS_BAD -> 100f - score
        MetricSemantics.HIGH_IS_GOOD -> score
    }.coerceIn(0f, 100f)

    fun interpretationHint(language: AppLanguage): String = when (language) {
        AppLanguage.KZ -> when (semantics) {
            MetricSemantics.HIGH_IS_BAD -> "Пайыз неғұрлым жоғары болса, осы тақырып бойынша қауіп соғұрлым айқынырақ болады."
            MetricSemantics.HIGH_IS_GOOD -> "Пайыз неғұрлым жоғары болса, осы тақырып бойынша жағдай соғұрлым тұрақты әрі жақсы болады."
        }
        else -> when (semantics) {
        MetricSemantics.HIGH_IS_BAD -> "Чем выше процент, тем сильнее выражен риск по этой теме."
        MetricSemantics.HIGH_IS_GOOD -> "Чем выше процент, тем устойчивее и лучше состояние по этой теме."
        }
    }

    fun buildPrompt(answers: List<StudentTestAnswer>, score: Int, assessment: String, language: AppLanguage): String {
        val answersBlock = answers.joinToString("\n") { answer ->
            val polarityLabel = if (answer.polarity == QuestionPolarity.POSITIVE) "positive" else "negative"
            "${answer.questionId}. ${when (language) {
                AppLanguage.KZ -> "Сұрақ"
                AppLanguage.EN -> "Question"
                AppLanguage.RU -> "Вопрос"
            }}: «${localizedQuestionText(answer.questionId, language)}»\n   ${when (language) {
                AppLanguage.KZ -> "Жауап"
                AppLanguage.EN -> "Answer"
                AppLanguage.RU -> "Ответ"
            }}: ${answer.answerType.displayLabel(language)} (weight: ${answer.weight}/4, polarity: $polarityLabel, cat: ${answer.catEmotion.name})"
        }
        val statusLabel = when (language) {
            AppLanguage.KZ -> when (assessment) {
                "normal" -> "қалыпты"
                "stress" -> "кернеу / назар аймағы"
                "critical" -> "айқын қауіп"
                else -> "анықталмаған"
            }
            AppLanguage.EN -> when (assessment) {
                "normal" -> "normal"
                "stress" -> "stress / attention zone"
                "critical" -> "high risk"
                else -> "undefined"
            }
            AppLanguage.RU -> when (assessment) {
                "normal" -> "норма"
                "stress" -> "напряжение / зона внимания"
                "critical" -> "выраженный риск"
                else -> "не определено"
            }
        }

        return when (language) {
            AppLanguage.KZ -> """
Сен AiPhysical қолданбасындағы студенттің шағын тест нәтижесін талдап отырсың.

Тест атауы: ${localizedTestName(language)}.
Тест мақсаты: ${localizedPurpose(language)}.
Мағыналық фокус: ${localizedFocusHint(language)}.

Төменде студенттің 10 сұраққа берген жауаптары:
$answersBlock

Техникалық қорытынды:
- metric score: $score/100
- алдын ала статус: $statusLabel
- шкала семантикасы: ${interpretationHint(language)}

Осы тесттің нәтижесін құрастыр, жай чат жауабын емес.
Жауап қарапайым, жылы және телефоннан оқуға өте түсінікті болсын.
3 қысқа абзац жаз.
Әр абзацта 1–2 қысқа сөйлем болсын.
Барлығы 5 қысқа сөйлемнен аспасын.
Алдымен жалпы жағдайды ата, кейін оның сезімде, ойда не әрекетте қалай көрінетінін көрсет, соңында бір нақты қорытынды мен бір нақты қадам бер.
Қазақ тілінде, "сен" деп жаз.
Диагноз қойма, қорқытпа, құрғақ сандарға сүйенбе, мәтін тірі әрі адамға жақын болсын.
            """.trimIndent()
            AppLanguage.EN -> """
You are analyzing a student's mini-test result in AiPhysical.

Test name: ${localizedTestName(language)}.
Goal: ${localizedPurpose(language)}.
Focus: ${localizedFocusHint(language)}.

Student answers:
$answersBlock

Technical summary:
- metric score: $score/100
- preliminary status: $statusLabel
- scale semantics: ${interpretationHint(language)}

Write the actual test result, not a generic chat reply.
Use warm, simple, phone-friendly language.
Format: 3 short paragraphs, 1-2 short sentences each, up to 5 short sentences total.
Describe the general state first, then how it may show up in feelings/thoughts/behavior, and finish with one concrete conclusion and one practical next step.
Avoid diagnosis, fear tactics, and dry formal language.
            """.trimIndent()
            AppLanguage.RU -> """
Ты анализируешь результат мини-теста студента в приложении AiPhysical.

Название теста: ${localizedTestName(language)}.
Цель теста: ${localizedPurpose(language)}.
Смысловой фокус: ${localizedFocusHint(language)}.

Ниже ответы студента на 10 вопросов:
$answersBlock

Технический итог:
- metric score: $score/100
- предварительный статус: $statusLabel
- семантика шкалы: ${interpretationHint(language)}

Сформируй именно результат этого теста, а не обычный чат-ответ.

Ответ должен быть простым, тёплым и очень понятным для чтения с экрана телефона.

Формат:
- 3 коротких абзаца;
- между абзацами одна пустая строка;
- в каждом абзаце 1–2 коротких предложения;
- максимум 5 коротких предложений суммарно.

Содержание:
- сначала коротко назови общее состояние человека;
- затем покажи, как это проявляется в ощущениях, мыслях или поведении;
- в конце дай один точный вывод и один конкретный шаг на сейчас.

Ограничения:
- русский язык;
- обращение на «ты»
- без сухих цифр и формальной расшифровки;
- без диагнозов и запугивания;
- без пафоса, без тяжёлых метафор и без литературного стиля;
- не пиши слишком длинными предложениями;
- текст должен быть живым, ясным и человечным.
            """.trimIndent()
        }
    }

    fun buildFallback(score: Int, assessment: String, language: AppLanguage): String = when (language) {
        AppLanguage.KZ -> buildKzFallback(assessment)
        else -> when (type) {
        StudentTestType.BURNOUT -> when (assessment) {
            "normal" -> "Хмм, я внимательно посмотрел на твои ответы: выраженного выгорания здесь не видно. Похоже, у тебя ещё сохраняется внутренний ресурс, и ты не потерял связь с тем, ради чего вообще стараешься. Важный нюанс в том, что такие состояния держатся не на силе воли, а на регулярном восстановлении. Прямо сейчас лучший ход — не ждать перегруза, а сознательно оставить себе короткую паузу без задач и чувства вины."
            "stress" -> "Хмм, картина выглядит так: ты ещё держишься, но усталость уже начала работать изнутри и понемногу съедать запас сил. Обычно в таком состоянии человек продолжает тянуть, хотя удовольствие и лёгкость уже заметно снижаются. Тонкий момент в том, что выгорание редко начинается с обвала — чаще оно приходит через привычку всё терпеть. Прямо сейчас полезнее всего немного снизить темп и вернуть себе хотя бы один нормальный кусок отдыха сегодня."
            else -> "Хмм, по этому тесту видно, что нагрузка для тебя сейчас уже не просто высокая, а истощающая. Похоже, ты слишком долго работаешь на внутреннем резерве, и организм начинает отвечать не мотивацией, а отстранением и усталостью. В таких состояниях особенно легко ошибочно решить, будто проблема в характере, хотя на деле речь про перегруз. Прямо сейчас самый разумный шаг — убрать хотя бы одну лишнюю задачу и дать себе восстановление раньше, чем станет ещё тяжелее."
        }

        StudentTestType.STRESS -> when (assessment) {
            "normal" -> "Хмм, по ответам видно, что напряжение у тебя есть, но оно пока не управляет всей системой. Обычно это признак того, что ты ещё умеешь переключаться и не разваливаешься под первой же перегрузкой. Главное наблюдение здесь простое: устойчивость держится на базовых вещах, а не на героизме. Прямо сейчас полезно сохранить этот запас и сделать сегодня одну осознанную паузу до того, как усталость начнёт диктовать условия."
            "stress" -> "Хмм, сейчас стресс у тебя уже не фоновый — он начинает вмешиваться и в мысли, и в телесные ощущения. В таком состоянии человек часто живёт в режиме постоянной внутренней спешки, даже когда формально уже можно остановиться. Тонкая проблема здесь в том, что перегруз быстро становится привычным и начинает казаться нормой. Прямо сейчас лучший шаг — на 10 минут выключиться из темпа: встать, пройтись, выдохнуть и вернуть телу ощущение, что опасность не везде."
            else -> "Хмм, по этой картине видно, что напряжение у тебя уже слишком плотное и почти не оставляет свободного пространства внутри. Когда стресс становится хроническим, человек начинает реагировать не по ситуации, а по уровню истощения — отсюда раздражение, зажимы и чувство, что всё слишком. Важно заметить: это не слабость, а перегруженная нервная система. Прямо сейчас стоит не ускоряться, а наоборот резко снизить темп хотя бы на ближайший вечер."
        }

        StudentTestType.EMOTION -> when (assessment) {
            "normal" -> "Хмм, эмоциональный фон у тебя сейчас выглядит достаточно живым и устойчивым. Это значит, что внутри ещё есть контакт не только с обязанностями, но и с интересом, теплом и ощущением вкуса к жизни. Важный инсайт здесь в том, что устойчивость часто незаметна самому человеку, пока она у него есть. Прямо сейчас стоит закрепить это состояние чем-то простым и приятным, что напомнит тебе: жизнь не сводится только к задачам."
            "stress" -> "Хмм, по ответам видно, что эмоциональная система у тебя сейчас работает тише обычного. Похоже, радость и включённость не исчезли полностью, но им стало сложнее пробиваться через усталость, рутину или внутреннее напряжение. Обычно это тот момент, когда человек ещё функционирует, но уже почти перестаёт чувствовать вкус происходящего. Прямо сейчас полезнее всего вернуть себе маленький живой контакт с чем-то приятным, а не пытаться просто «собраться»."
            else -> "Хмм, сейчас общая эмоциональная картина выглядит так, будто ты давно живёшь на автопилоте и почти не получаешь внутренней отдачи от происходящего. В подобных состояниях человек может продолжать делать всё как надо, но внутри постепенно теряет ощущение движения и смысла. Самое важное здесь — не путать эмоциональное истощение с собственной «ленивостью» или «неправильностью». Прямо сейчас стоит дать себе один маленький, но живой источник отклика: прогулку, музыку, разговор или смену среды."
        }

        StudentTestType.MOTIVATION -> when (assessment) {
            "normal" -> "Хмм, здесь видно хорошую вещь: у тебя пока не потеряна внутренняя сцепка с тем, что ты делаешь. Даже если усталость появляется, смысл и интерес всё ещё работают как опора, а это гораздо важнее, чем просто дисциплина. Часто человек недооценивает, насколько далеко его ведёт не жёсткость, а ясность «зачем». Прямо сейчас полезно выбрать один следующий шаг и сделать его без попытки охватить всё сразу."
            "stress" -> "Хмм, мотивация у тебя сейчас не исчезла, но стала неровной и слишком зависимой от уровня усталости. Обычно это выглядит так: задачи вроде важны, но вход в них требует слишком большого внутреннего разгона, поэтому появляется откладывание. Инсайт здесь в том, что проблема, похоже, не в отсутствии целей, а в перегрузке между тобой и этими целями. Прямо сейчас лучшее решение — выбрать одну маленькую задачу, которую можно закрыть без внутренней борьбы."
            else -> "Хмм, по этой картине видно, что мотивация у тебя сейчас заметно просела и движение вперёд ощущается скорее как давление, чем как выбор. В таких состояниях человек часто начинает считать себя ленивым, хотя на деле у него просто пропадает ощущение смысла и достижимости. Самое важное наблюдение: искра редко возвращается под нажимом, ей нужен воздух и понятная опора. Прямо сейчас попробуй сузить горизонт до одного маленького действия, которое реально завершить сегодня."
        }

        StudentTestType.ANXIETY -> when (assessment) {
            "normal" -> "Хмм, по этому тесту видно, что тревога у тебя не исчезла совсем, но пока не захватывает управление. Это обычно означает, что внутри ещё есть опора и способность различать реальные задачи и лишнее внутреннее накручивание. Интересный момент в том, что спокойствие редко ощущается как что-то заметное, пока оно есть. Прямо сейчас полезно закрепить это состояние одним простым действием из зоны контроля, а не распыляться на всё сразу."
            "stress" -> "Хмм, тревога у тебя сейчас выглядит уже достаточно активной и явно влияет на внутренний фон. Похоже, часть энергии уходит не на реальные действия, а на прокручивание вариантов, перепроверки и попытку заранее обезвредить будущее. Главный инсайт здесь в том, что тревога часто маскируется под ответственность, хотя на деле только сильнее перегружает систему. Прямо сейчас стоит сузить фокус до одного понятного шага, который реально зависит от тебя в ближайший час."
            else -> "Хмм, по этой картине видно, что тревога сейчас у тебя держится слишком близко к поверхности и почти не даёт системе по-настоящему расслабиться. В таком режиме мысли часто начинают работать не как инструмент, а как бесконечный сканер угроз — и это очень выматывает. Важно заметить: это не про слабость, а про перегруженную внутреннюю сигнализацию. Прямо сейчас лучший ход — остановиться и вернуть внимание в тело: выдох, опора ногами в пол и только одна задача вместо десяти сразу."
        }
        }
    }

    fun localizedTestName(language: AppLanguage): String = when (language) {
        AppLanguage.RU -> testName
        AppLanguage.EN -> type.displayLabel(language)
        AppLanguage.KZ -> type.displayLabel(language)
    }

    private fun localizedPurpose(language: AppLanguage): String = when (language) {
        AppLanguage.KZ -> when (type) {
            StudentTestType.BURNOUT -> "Оқу мен жүктеме аясындағы эмоциялық күйіп кету, қажу және ресурс жоғалту белгілерін бағалау."
            StudentTestType.STRESS -> "Жүктеме мен дедлайндарға байланысты қазіргі физикалық және психикалық кернеу деңгейін бағалау."
            StudentTestType.EMOTION -> "Жалпы эмоциялық фонды, өмірге қанағаттануды және күнделікті қуанышпен байланысты бағалау."
            StudentTestType.MOTIVATION -> "Оқу мен жобаларға тартылуды, ішкі мағынаны және кейінге қалдыру деңгейін бағалау."
            StudentTestType.ANXIETY -> "Мазасыздық, шамадан тыс уайымдау және болашақ алдындағы кернеу деңгейін бағалау."
        }
        else -> purpose
    }

    private fun localizedFocusHint(language: AppLanguage): String = when (language) {
        AppLanguage.KZ -> when (type) {
            StudentTestType.BURNOUT -> "Күйіп кету, оқу шаршауы, қажу, ресурсты қалпына келтіру және демалыс туралы кеңестер."
            StudentTestType.STRESS -> "Физикалық және психикалық кернеу, дедлайндар, үзіліс, қарқын және қалпына келу."
            StudentTestType.EMOTION -> "Эмоциялық фон, өмірге қанағаттану, қуаныш, қызығушылық және күнделікті тіректер."
            StudentTestType.MOTIVATION -> "Тартылу, мағына, прокрастинация, мақсаттар және кішкентай қадамдар."
            StudentTestType.ANXIETY -> "Мазасыздық, ойды ширату, болашақ алдындағы кернеу, grounding және бақылау аймағы."
        }
        else -> focusHint
    }

    fun localizedQuestionText(questionId: Int, language: AppLanguage): String {
        if (language != AppLanguage.KZ) {
            return questions.firstOrNull { it.id == questionId }?.text.orEmpty()
        }
        return when (type) {
            StudentTestType.BURNOUT -> when (questionId) {
                1 -> "Таңертең өзімді сергек сезініп, тапсырмаларға дайын боламын"
                2 -> "Бұрын ашуландырмайтын адамдар мені тітіркендіре бастады"
                3 -> "Оқуымның шынайы пайда әкелетінін сеземін"
                4 -> "Күннің соңында өзімді бос қалған стакандай сезінемін"
                5 -> "Оқудан кейін хобби мен достарға күшім жетеді"
                6 -> "Істер туралы ойлар демалыста босаңсуыма кедергі жасайды"
                7 -> "Компромиссті оңай тауып, адамдарға ашуланбаймын"
                8 -> "Кез келген ұсақ тапсырма маған үлкен таудай көрінеді"
                9 -> "Достарыммен сөйлескенде жиі күлемін не жымиямын"
                10 -> "Бәрі мені жайыма қалдырса екен деп қалаймын"
                else -> ""
            }
            StudentTestType.STRESS -> when (questionId) {
                1 -> "Мен жеткілікті ұйықтаймын және таңертең сергек оянамын"
                2 -> "Іс көбейгенде бұлшықеттерім жиі тартылады немесе басым ауырады"
                3 -> "Оқудан демалысқа оңай ауысып, ұйықтар алдында дедлайндарды ойламаймын"
                4 -> "Кестедегі кез келген күтпеген өзгеріс маған үрей не қатты ашу тудырады"
                5 -> "Жүйкем тозғанда тез тынышталуға көмектесетін тәсілдерім бар"
                6 -> "Ұсақ нәрселер үшін жақындарыма не топтастарыма жиі ашуланатын болдым"
                7 -> "Күнім өте тығыз болса да, қалыпты түскі асқа уақыт табамын"
                8 -> "Маған үнемі бір жерге кешігіп, ешнәрсеге үлгермей жатқандай көрінеді"
                9 -> "Мұғалімнен не командадан келген түзетулерді ренішсіз қабылдай аламын"
                10 -> "Кешке қарай кернеу соншалық күшейеді, бәрінен тығылғым келеді"
                else -> ""
            }
            StudentTestType.EMOTION -> when (questionId) {
                1 -> "Жалпы алғанда, қазіргі өмірімнің қалай өтіп жатқанына көңілім толады"
                2 -> "Соңғы кезде себепсіз жиі мұңайып қаламын"
                3 -> "Жақсы көретін ісіммен айналысу үшін демалыс күндерін асыға күтемін"
                4 -> "Көп уақытта жарқын эмоциясыз, автопилотта жүргендей боламын"
                5 -> "Айналамда еркін әрі жағымды сөйлесетін адамдар бар"
                6 -> "Мені ешкім шын мәнінде түсінбейтіндей сезінемін"
                7 -> "Дәмді кофе, жақсы ауа райы, сәтті әзіл сияқты жақсы ұсақ нәрселерді байқай аламын"
                8 -> "Кейде айналамның бәріне қатты ашуланып, бәрін тастағым келеді"
                9 -> "Қазіргі өмірлік қиындықтарды еңсеруге өзімде күш барын сеземін"
                10 -> "Мен бір орында тұрып қалғандаймын және ештеңе өзгермейтіндей көрінеді"
                else -> ""
            }
            StudentTestType.MOTIVATION -> when (questionId) {
                1 -> "Мамандығым бойынша жаңа тақырыптарды оқуға шын қызығамын"
                2 -> "Тапсырмаларды тек баға үшін не менен қойсын деп орындаймын"
                3 -> "Қазір алып жатқан білімімнің не үшін қажет екенін анық түсінемін"
                4 -> "Қиын тапсырмаларды үнемі ең соңына қалдырамын"
                5 -> "Күрделі тапсырманы шешкенде қанағат сезінемін"
                6 -> "Қанша тырыссам да, оны ешкім бағаламайтындай көрінеді"
                7 -> "Қызықты тақырыптар бойынша қосымша ақпаратты өзім іздеймін"
                8 -> "Әр таң сайын өзімді оқуға не жобаға отыруға зорлаймын"
                9 -> "Мені шабыттандырып, алға жетелейтін үлкен мақсаттарым бар"
                10 -> "Дұрыс мамандық не бағыт таңдадым ба деп жиі күмәнданамын"
                else -> ""
            }
            StudentTestType.ANXIETY -> when (questionId) {
                1 -> "Осы аптада туындайтын мәселелердің көбін шеше алатыныма сенімдімін"
                2 -> "Ең жаман сценарийлерді елестетіп, өзімді жиі уайымға саламын"
                3 -> "Маңызды оқиғалар алдында уайымдаймын, бірақ бұл әрекет етуіме кедергі емес"
                4 -> "Кейде айқын физикалық себепсіз жүрегім жиі соға бастайды"
                5 -> "Әсер ете алмайтын жағдайларды жібере аламын"
                6 -> "Өткен әңгімелерге қайта оралып, басқаша жауап беруім керек еді деп ойлаймын"
                7 -> "Түнде мазасыз ойлардың шексіз ағынынсыз тыныш ұйықтап кетемін"
                8 -> "Болашағымдағы белгісіздік мені қатты қорқытады"
                9 -> "Бейтаныс ортада өзімді жайлы сезініп, әңгіме қолдай аламын"
                10 -> "Кейде өзіме сенбей, бір нәрсені бірнеше рет қайта тексеремін"
                else -> ""
            }
        }
    }

    private fun buildKzFallback(assessment: String): String = when (type) {
        StudentTestType.BURNOUT -> when (assessment) {
            "normal" -> "Жауаптарыңа қарағанда, айқын күйіп кету байқалмайды. Ішкі ресурсың әлі сақталған сияқты. Қазір өзіңе қысқа демалыс беріп, күшті алдын ала толықтырған дұрыс."
            "stress" -> "Қазір шаршау жиналып келе жатқаны байқалады. Сен әлі ұстап тұрсың, бірақ жеңілдік азайып барады. Бүгін қарқыныңды сәл төмендетіп, өзіңе қалыпты демалыс уақытын қайтарған пайдалы болады."
            else -> "Бұл тест жүктеменің сені қатты қажытып жатқанын көрсетеді. Мұндай кезде мәселе мінезде емес, шамадан тыс жүктемеде болады. Қазір ең дұрыс қадам — кем дегенде бір артық міндетті алып тастап, қалпына келуге уақыт беру."
        }
        StudentTestType.STRESS -> when (assessment) {
            "normal" -> "Кернеу бар, бірақ ол әзірге бәрін толық басқармайды. Демек, сенде әлі де тұрақтылық қоры бар. Оны сақтау үшін бүгін саналы түрде қысқа үзіліс жаса."
            "stress" -> "Стресс енді фондық емес, ойың мен денеңе де әсер ете бастаған сияқты. Мұндайда артық жүктеме қалыпты нәрседей сезіле бастайды. Қазір 10 минутқа қарқынды тоқтатып, тыныс алып, денеге қауіптің барлық жерде емес екенін сездіру маңызды."
            else -> "Кернеу тым тығыз болып, ішкі бос кеңістікті қалдырмай тұрғандай. Бұл әлсіздік емес, жүйкенің шамадан тыс жүктелуі. Қазір жылдамдаудың орнына, кем дегенде осы кешке қарқынды күрт азайтқан дұрыс."
        }
        StudentTestType.EMOTION -> when (assessment) {
            "normal" -> "Эмоциялық фон қазір жеткілікті тұрақты көрінеді. Демек, сенде қызығушылық пен жылылықпен байланыс сақталған. Осыны бекіту үшін бүгін өзіңе кішкентай жағымды сәт сыйла."
            "stress" -> "Қазір эмоциялық жүйе әдеттегіден бәсең жұмыс істеп тұрған сияқты. Қуаныш жоғалмаған, бірақ оған жету қиындаған. Өзіңді жай жинауға тырысқаннан гөрі, жағымды бір тірі әсерді қайта қосқан пайдалырақ болады."
            else -> "Қазір өмір автопилотта өтіп жатқандай сезілуі мүмкін. Бұл жалқаулық емес, эмоциялық қажу белгісі. Қазір серуен, музыка, әңгіме немесе ортаны ауыстыру сияқты кішкентай тірі әсер қажет."
        }
        StudentTestType.MOTIVATION -> when (assessment) {
            "normal" -> "Істеп жүрген нәрсеңмен ішкі байланыс әлі сақталған. Бұл жай тәртіптен де маңыздырақ тірек. Қазір бәрін бірден қамтымай, бір нақты келесі қадамды таңдаған жақсы."
            "stress" -> "Мотивация жоғалмаған, бірақ шаршауға қатты тәуелді болып қалған сияқты. Кіруі қиын тапсырмалар кейінге шегеріле береді. Қазір ішкі күрессіз бітіре алатын бір кішкентай істі таңдау тиімді болады."
            else -> "Мотивация айтарлықтай төмендегені көрінеді. Бұл жалқаулық емес — мағына мен қолжетімділік сезімінің әлсіреуі. Қазір горизонтыңды тарылтып, бүгін аяқтауға болатын бір өте шағын әрекеттен баста."
        }
        StudentTestType.ANXIETY -> when (assessment) {
            "normal" -> "Мазасыздық бар, бірақ ол әзірге толық бақылауды алған жоқ. Демек, ішкі тірегің сақталған. Оны күшейту үшін дәл қазір бақылауыңдағы бір қарапайым қадам жаса."
            "stress" -> "Қазір мазасыздық ішкі фонға анық әсер етіп тұр. Күштің бір бөлігі нақты іске емес, ойша алдын ала тексеруге кетіп жатқан сияқты. Фокусты өзіңе тәуелді бір ғана жақын қадамға тарылтқан дұрыс."
            else -> "Мазасыздық тым бетке жақын болып, жүйені босаңсуға жібермей тұрғандай. Бұл әлсіздік емес, ішкі дабылдың шамадан тыс жүктелуі. Қазір тоқтап, тынысты баяулатып, денеге тірек беріп, он істің орнына бір істі ғана қалдыр."
        }
    }
}

data class StudentTestSubmission(
    val definition: StudentTestDefinition,
    val score: Int,
    val aiAssessment: String,
    val feedbackText: String,
    val answers: List<StudentTestAnswer>,
    val version: Int = 1
)

data class StudentMetricSummary(
    val type: StudentTestType,
    val label: String,
    val score: Float,
    val semantics: MetricSemantics,
    val isCompleted: Boolean,
    val healthContribution: Float?,
    val aiAssessment: String?
)

private const val CRITICAL_THRESHOLD = 70
private const val RISK_STRESS_THRESHOLD = 40
private const val POSITIVE_NORMAL_THRESHOLD = 70
private const val POSITIVE_STRESS_THRESHOLD = 40

private fun q(
    id: Int,
    text: String,
    catEmotion: CatState,
    polarity: QuestionPolarity
) = StudentTestQuestion(id, text, catEmotion, polarity)

private val burnoutDefinition = StudentTestDefinition(
    type = StudentTestType.BURNOUT,
    testId = "burnout",
    testName = "Выгорание",
    profileField = "burnoutScore",
    semantics = MetricSemantics.HIGH_IS_BAD,
    purpose = "Оценить признаки эмоционального выгорания, истощения и потери ресурса на фоне учёбы и нагрузки.",
    focusHint = "Эмоциональное выгорание, усталость от учёбы, истощение, потеря ресурса, советы про восстановление и отдых.",
    questions = listOf(
        q(1, "Утром я чувствую себя бодрым и готовым к задачам", CatState.HAPPY, QuestionPolarity.POSITIVE),
        q(2, "Меня начали бесить люди, которые раньше не раздражали", CatState.NERVOUS, QuestionPolarity.NEGATIVE),
        q(3, "Я чувствую, что моя учёба приносит реальную пользу", CatState.PROUD, QuestionPolarity.POSITIVE),
        q(4, "В конце дня я чувствую себя как пустой стакан", CatState.TIRED, QuestionPolarity.NEGATIVE),
        q(5, "У меня хватает сил на хобби и друзей после учёбы", CatState.ENERGETIC, QuestionPolarity.POSITIVE),
        q(6, "Мысли о делах не дают мне расслабиться на отдыхе", CatState.STRESS, QuestionPolarity.NEGATIVE),
        q(7, "Я легко нахожу компромиссы и не злюсь на людей", CatState.PEACEFUL, QuestionPolarity.POSITIVE),
        q(8, "Любая мелкая задача кажется мне огромной горой", CatState.OVERWHELMED, QuestionPolarity.NEGATIVE),
        q(9, "Я часто улыбаюсь или смеюсь при общении с друзьями", CatState.LAUGHING, QuestionPolarity.POSITIVE),
        q(10, "Я хочу, чтобы меня просто все оставили в покое", CatState.IN_BOX, QuestionPolarity.NEGATIVE)
    )
)

private val stressDefinition = StudentTestDefinition(
    type = StudentTestType.STRESS,
    testId = "stress",
    testName = "Стресс",
    profileField = "stressScore",
    semantics = MetricSemantics.HIGH_IS_BAD,
    purpose = "Оценить текущий уровень физического и ментального напряжения, связанного с нагрузкой и дедлайнами.",
    focusHint = "Физическое и ментальное напряжение, дедлайны, перегрузка, телесные симптомы напряжения, советы про паузы, темп и восстановление.",
    questions = listOf(
        q(1, "Я сплю достаточно, и по утрам чувствую себя отдохнувшим.", CatState.ENERGETIC, QuestionPolarity.POSITIVE),
        q(2, "Из-за наплыва дел у меня часто напряжены мышцы (шея, плечи) или болит голова.", CatState.STRESS, QuestionPolarity.NEGATIVE),
        q(3, "Я легко переключаюсь с учебы на отдых и не думаю о дедлайнах перед сном.", CatState.PEACEFUL, QuestionPolarity.POSITIVE),
        q(4, "Любое внезапное изменение в расписании вызывает у меня панику или сильное раздражение.", CatState.OVERWHELMED, QuestionPolarity.NEGATIVE),
        q(5, "У меня есть проверенные способы быстро успокоиться, если я нервничаю.", CatState.PROUD, QuestionPolarity.POSITIVE),
        q(6, "Я стал(а) чаще срываться на близких или однокурсников из-за мелочей.", CatState.NERVOUS, QuestionPolarity.NEGATIVE),
        q(7, "Я нахожу время для нормального обеда, даже когда день очень загружен.", CatState.HAPPY, QuestionPolarity.POSITIVE),
        q(8, "Мне кажется, что я всё время куда-то опаздываю и ничего не успеваю.", CatState.STRESS, QuestionPolarity.NEGATIVE),
        q(9, "Я могу спокойно выслушать правки от преподавателя/команды без обид.", CatState.PEACEFUL, QuestionPolarity.POSITIVE),
        q(10, "К вечеру я чувствую такое напряжение, что хочется просто спрятаться от всех.", CatState.IN_BOX, QuestionPolarity.NEGATIVE)
    )
)

private val emotionDefinition = StudentTestDefinition(
    type = StudentTestType.EMOTION,
    testId = "emotion",
    testName = "Состояние",
    profileField = "emotionScore",
    semantics = MetricSemantics.HIGH_IS_GOOD,
    purpose = "Оценить общий эмоциональный фон, удовлетворённость жизнью и контакт с повседневной радостью.",
    focusHint = "Общий эмоциональный фон, удовлетворённость жизнью, радость, интерес, контакт с окружением, повседневные маленькие опоры.",
    questions = listOf(
        q(1, "В целом, я доволен(на) тем, как сейчас складывается моя жизнь.", CatState.HAPPY, QuestionPolarity.POSITIVE),
        q(2, "В последнее время мне часто бывает грустно без видимой на то причины.", CatState.TIRED, QuestionPolarity.NEGATIVE),
        q(3, "Я с нетерпением жду выходных, чтобы заняться тем, что люблю.", CatState.ENERGETIC, QuestionPolarity.POSITIVE),
        q(4, "Большую часть времени я чувствую себя «на автопилоте», не испытывая ярких эмоций.", CatState.IDLE, QuestionPolarity.NEGATIVE),
        q(5, "В моем окружении есть люди, с которыми мне легко и приятно общаться.", CatState.LAUGHING, QuestionPolarity.POSITIVE),
        q(6, "Мне кажется, что никто меня по-настоящему не понимает.", CatState.IN_BOX, QuestionPolarity.NEGATIVE),
        q(7, "Я умею замечать хорошие мелочи: вкусный кофе, хорошую погоду, удачную шутку.", CatState.PEACEFUL, QuestionPolarity.POSITIVE),
        q(8, "Иногда я так злюсь на всё вокруг, что хочется всё бросить.", CatState.NERVOUS, QuestionPolarity.NEGATIVE),
        q(9, "Я чувствую в себе силы справляться с текущими жизненными трудностями.", CatState.PROUD, QuestionPolarity.POSITIVE),
        q(10, "У меня ощущение, что я застрял(а) в «дне сурка» и ничего не меняется.", CatState.OVERWHELMED, QuestionPolarity.NEGATIVE)
    )
)

private val motivationDefinition = StudentTestDefinition(
    type = StudentTestType.MOTIVATION,
    testId = "motivation",
    testName = "Мотивация",
    profileField = "motivationScore",
    semantics = MetricSemantics.HIGH_IS_GOOD,
    purpose = "Оценить вовлечённость в учёбу и проекты, внутренний смысл усилий и уровень прокрастинации.",
    focusHint = "Вовлечённость в учёбу и проекты, смысл, прокрастинация, цели и движение вперёд, советы про маленькие шаги и внутреннюю опору.",
    questions = listOf(
        q(1, "Мне искренне интересно изучать новые темы по моей специальности.", CatState.ENERGETIC, QuestionPolarity.POSITIVE),
        q(2, "Я делаю задания только ради оценки или чтобы от меня отстали преподаватели.", CatState.TIRED, QuestionPolarity.NEGATIVE),
        q(3, "Я четко понимаю, зачем мне нужно то образование, которое я сейчас получаю.", CatState.PROUD, QuestionPolarity.POSITIVE),
        q(4, "Я постоянно откладываю сложные задачи на самый последний момент (прокрастинирую).", CatState.NERVOUS, QuestionPolarity.NEGATIVE),
        q(5, "Когда я решаю сложную задачу (например, фиксирую баг в коде), я испытываю удовольствие.", CatState.HAPPY, QuestionPolarity.POSITIVE),
        q(6, "Мне кажется, что мои усилия всё равно никто не оценит, так зачем стараться?", CatState.IN_BOX, QuestionPolarity.NEGATIVE),
        q(7, "Я сам(а) ищу дополнительную информацию по темам, которые мне интересны.", CatState.ENERGETIC, QuestionPolarity.POSITIVE),
        q(8, "Каждое утро мне приходится буквально заставлять себя садиться за учебу или проект.", CatState.OVERWHELMED, QuestionPolarity.NEGATIVE),
        q(9, "У меня есть большие цели на будущее, которые меня зажигают и заставляют двигаться.", CatState.PROUD, QuestionPolarity.POSITIVE),
        q(10, "Я часто чувствую сомнение, что выбрал(а) правильную профессию или направление.", CatState.TIRED, QuestionPolarity.NEGATIVE)
    )
)

private val anxietyDefinition = StudentTestDefinition(
    type = StudentTestType.ANXIETY,
    testId = "anxiety",
    testName = "Тревожность",
    profileField = "anxietyScore",
    semantics = MetricSemantics.HIGH_IS_BAD,
    purpose = "Оценить уровень тревоги, накручивания, напряжённости перед будущим и сложности с расслаблением.",
    focusHint = "Тревога, накручивание, напряжённость перед будущим, социальная тревога, трудности с расслаблением, советы про grounding и зону контроля.",
    questions = listOf(
        q(1, "Я уверен(а), что смогу решить большинство проблем, которые возникнут на этой неделе.", CatState.PROUD, QuestionPolarity.POSITIVE),
        q(2, "Я часто «накручиваю» себя, представляя самые худшие варианты развития событий.", CatState.OVERWHELMED, QuestionPolarity.NEGATIVE),
        q(3, "Перед важными событиями (экзамен, защита хакатона) я волнуюсь, но это не мешает мне действовать.", CatState.ENERGETIC, QuestionPolarity.POSITIVE),
        q(4, "У меня бывают моменты, когда сердце начинает биться чаще без объективной физической причины.", CatState.STRESS, QuestionPolarity.NEGATIVE),
        q(5, "Я умею отпускать ситуации, на которые никак не могу повлиять.", CatState.PEACEFUL, QuestionPolarity.POSITIVE),
        q(6, "Я постоянно возвращаюсь к прошлым разговорам и думаю: «надо было ответить/сделать иначе».", CatState.NERVOUS, QuestionPolarity.NEGATIVE),
        q(7, "Я спокойно засыпаю и не кручу в голове бесконечный поток тревожных мыслей.", CatState.HAPPY, QuestionPolarity.POSITIVE),
        q(8, "Меня сильно пугает неопределенность в моем будущем (учеба, карьера, жизнь).", CatState.IN_BOX, QuestionPolarity.NEGATIVE),
        q(9, "В незнакомой компании я чувствую себя вполне комфортно и могу поддержать беседу.", CatState.LAUGHING, QuestionPolarity.POSITIVE),
        q(10, "Иногда я проверяю одни и те же вещи по нескольку раз (сохранил ли код, закрыл ли дверь), потому что не доверяю себе.", CatState.NERVOUS, QuestionPolarity.NEGATIVE)
    )
)

val STUDENT_TEST_DEFINITIONS: Map<StudentTestType, StudentTestDefinition> = listOf(
    burnoutDefinition,
    stressDefinition,
    emotionDefinition,
    motivationDefinition,
    anxietyDefinition
).associateBy { it.type }

fun studentTestDefinitionFor(type: StudentTestType): StudentTestDefinition =
    STUDENT_TEST_DEFINITIONS.getValue(type)

fun scoreForTest(type: StudentTestType, score: Float, completedTestIds: Set<String>): Float? {
    return if (type.testId in completedTestIds) score.coerceIn(0f, 100f) else null
}

fun buildStudentMetricSummaries(profile: UserProfile, completedTestIds: Set<String>, language: AppLanguage = AppLanguage.RU): List<StudentMetricSummary> {
    return StudentTestType.entries.map { type ->
        val definition = studentTestDefinitionFor(type)
        val rawScore = when (type) {
            StudentTestType.BURNOUT -> profile.burnoutScore
            StudentTestType.STRESS -> profile.stressScore
            StudentTestType.EMOTION -> profile.emotionScore
            StudentTestType.MOTIVATION -> profile.motivationScore
            StudentTestType.ANXIETY -> profile.anxietyScore
        }.coerceIn(0f, 100f)
        val isCompleted = type.testId in completedTestIds
        StudentMetricSummary(
            type = type,
            label = type.displayLabel(language),
            score = rawScore,
            semantics = definition.semantics,
            isCompleted = isCompleted,
            healthContribution = if (isCompleted) definition.healthContribution(rawScore) else null,
            aiAssessment = if (isCompleted) definition.computeAssessment(rawScore.roundToInt()) else null
        )
    }
}

fun computeOverallHealthPercent(profile: UserProfile, completedTestIds: Set<String>): Float {
    val completedMetrics = buildStudentMetricSummaries(profile, completedTestIds)
        .mapNotNull { it.healthContribution }
    if (completedMetrics.isEmpty()) return 0f
    return (completedMetrics.average().toFloat()).coerceIn(0f, 100f)
}

fun computeAggregatedAiStatus(profile: UserProfile, completedTestIds: Set<String>): String {
    val metrics = buildStudentMetricSummaries(profile, completedTestIds).filter { it.isCompleted }
    if (metrics.isEmpty()) return "unknown"

    val overall = computeOverallHealthPercent(profile, completedTestIds)
    val hasCritical = metrics.any { it.aiAssessment == "critical" }
    val hasStress = metrics.any { it.aiAssessment == "stress" }

    return when {
        hasCritical || overall < 35f -> "critical"
        hasStress || overall < 60f -> "stress"
        else -> "normal"
    }
}

fun UserProfile.withUpdatedMetric(testId: String, score: Float): UserProfile = when (testId) {
    "burnout" -> copy(burnoutScore = score.coerceIn(0f, 100f))
    "stress" -> copy(stressScore = score.coerceIn(0f, 100f))
    "emotion" -> copy(emotionScore = score.coerceIn(0f, 100f))
    "motivation" -> copy(motivationScore = score.coerceIn(0f, 100f))
    "anxiety" -> copy(anxietyScore = score.coerceIn(0f, 100f))
    else -> this
}

fun lastTestAtFieldNameFor(testId: String): String? = when (testId) {
    "burnout" -> "lastBurnoutTestAt"
    "stress" -> "lastStressTestAt"
    "emotion" -> "lastEmotionTestAt"
    "motivation" -> "lastMotivationTestAt"
    "anxiety" -> "lastAnxietyTestAt"
    else -> null
}

// ── Burnout compatibility aliases ──────────────────────────────────────────────
typealias BurnoutQuestion = StudentTestQuestion
typealias BurnoutAnswer = StudentTestAnswer
typealias BurnoutTestStep = StudentTestStep
typealias BurnoutTestUiState = StudentTestUiState

object BurnoutScoring {
    const val NORMAL_THRESHOLD = POSITIVE_NORMAL_THRESHOLD
    const val STRESS_THRESHOLD = RISK_STRESS_THRESHOLD

    fun computeScore(answers: List<BurnoutAnswer>): Int = burnoutDefinition.scoreAnswers(answers)
    fun computeAssessment(score: Int): String = burnoutDefinition.computeAssessment(score)
}

val BURNOUT_QUESTIONS: List<BurnoutQuestion>
    get() = burnoutDefinition.questions

