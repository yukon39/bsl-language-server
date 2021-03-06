#Область ПрограммныйИнтерфейс

#Область ДочерняяОбласть

// Проверяет является ли вид ремонта бесплатным
//
// Параметры:
//  ВидРемонта - СправочникСсылка.ВидыРемонта - Проверяемый вид ремотна.
//
// Возвращаемое значение:
//  Булево.
//  Истина - Вид ремонта бестлатный.
//  Ложь - Вид ремонта платный или пустой.
//
Функция ЭтоБесплатный(ВидРемонта) Экспорт

	Если НЕ ЗначениеЗаполнено(ВидРемонта) Тогда

		Возврат Ложь;

	КонецЕсли;

	Возврат ЭтоКомплектация(ВидРемонта)
		ИЛИ (ОбщегоНазначения.ЗначениеРеквизитаОбъекта(ВидРемонта, "ТипРемонта") = Перечисления.ТипыРемонта.Бесплатный);

КонецФункции

// Проверяет является ли вид ремонта "КомплектацияАвтомобиля"
//
// Параметры:
//  ВидРемонта - СправочникСсылка.ВидыРемонта - Проверяемый вид ремонта.
//
// Возвращаемое значение:
//  Булево.
//
Функция ЭтоКомплектация(ВидРемонта) Экспорт

	Возврат (ВидРемонта = Справочники.ВидыРемонта.КомплектацияАвтомобиля);

КонецФункции

Функция БезОписанияНомер1() Экспорт

    Возврат Неопределено;

КонецФункции

Функция БезОписанияНоНеЭкспортная()

    Возврат Неопределено;

КонецФункции

#КонецОбласти

Функция БезОписанияНомер2() Экспорт

    Возврат Неопределено;

КонецФункции

#КонецОбласти

#Область СлужебныеПроцедурыИФункции

// Набор уровней для запросов в режиме совместимости с КЛАДР.
//
// Возвращаемое значение:
//     ФиксированныйМассив - набор числовых уровней.
//
Функция УровниКлассификатораКЛАДР() Экспорт

	Уровни = Новый Массив;
	Уровни.Добавить(1);
	Уровни.Добавить(3);
	Уровни.Добавить(4);
	Уровни.Добавить(6);
	Уровни.Добавить(7);

	Возврат Новый ФиксированныйМассив(Уровни);
КонецФункции

// Набор уровней для запросов ФИАС.
//
// Возвращаемое значение:
//     ФиксированныйМассив - набор числовых уровней.
//
Функция УровниКлассификатораФИАС() Экспорт

	Уровни = Новый Массив;
	Уровни.Добавить(1);
	Уровни.Добавить(2);
	Уровни.Добавить(3);
	Уровни.Добавить(5);
	Уровни.Добавить(4);
	Уровни.Добавить(6);
	Уровни.Добавить(7);
	Уровни.Добавить(90);
	Уровни.Добавить(91);

	Возврат Новый ФиксированныйМассив(Уровни);
КонецФункции

Функция БезОписанияНомер3() Экспорт

    Возврат Неопределено;

КонецФункции

#КонецОбласти
