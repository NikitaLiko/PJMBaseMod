# Документация по командам Project Minecraft Base Mod (PJM)

## Обзор системы команд

Мод использует иерархическую систему команд на основе Brigadier с корневой командой `/pjm` и множеством подкоманд, организованных по функциональности. Система поддерживает интеграцию с LuckPerms и использует уровни OP в качестве fallback.

## Корневая команда

### `/pjm`
Основная команда мода. Без аргументов выводит справку по всем доступным подкомандам.

| Подкоманда | Права | Описание |
|------------|-------|----------|
| `reload` | OP (2) | Перезагрузить конфигурацию мода |
| `help` | Все | Показать справку по командам |

---

## Управление игроками

### `/pjm player <игрок> <действие>`
**Требует:** OP (уровень 2) или разрешение `Pjmbasemod.team.manage`

Полное управление данными игроков, включая команды, классы, ранги и очки.

| Действие | Синтаксис | Описание |
|----------|-----------|----------|
| `team` | `/pjm player <игрок> team <team1\|team2>` | Установить команду игрока |
| `class` | `/pjm player <игрок> class <classId>` | Установить класс игрока |
| `rank` | `/pjm player <игрок> rank <rankId>` | Установить ранг игрока |
| `points add` | `/pjm player <игрок> points add <число>` | Добавить очки ранга |
| `points set` | `/pjm player <игрок> points set <число>` | Установить очки ранга |
| `info` | `/pjm player <игрок> info` | Показать полную информацию об игроке |
| `openmenu class` | `/pjm player <игрок> openmenu class` | Принудительно открыть меню выбора класса |
| `openmenu team` | `/pjm player <игрок> openmenu team` | Принудительно открыть меню выбора команды |
| `openmenu spawn` | `/pjm player <игрок> openmenu spawn` | Принудительно открыть меню спавна |

**Примеры:**
```
/pjm player Steve team team1
/pjm player Steve class assault
/pjm player Steve rank sergeant
/pjm player Steve points add 100
/pjm player Steve openmenu class
```

---

## Управление командами

### `/pjm team <действие>`
**Требует:** OP (уровень 2) или разрешение `Pjmbasemod.team.manage`

Система управления командами с автоматическим балансом и статистикой.

| Действие | Синтаксис | Описание |
|----------|-----------|----------|
| `balance` | `/pjm team balance` | Показать детальный баланс команд |
| `swap` | `/pjm team swap <игрок>` | Переместить игрока в противоположную команду |
| `shuffle` | `/pjm team shuffle` | Автоматически перемешать игроков между командами |

**Функции баланса:**
- Показывает количество игроков в каждой команде
- Отображает настроенный порог дисбаланса
- Предупреждает о критическом дисбалансе

**Примеры:**
```
/pjm team balance
/pjm team swap Steve
/pjm team shuffle
```

---

## Зоны выбора классов

### `/pjm zone <действие>`
**Требует:** OP (уровень 2) или разрешение `Pjmbasemod.class.zone.manage`

Управление зонами, где игроки могут выбирать классы.

| Действие | Синтаксис | Описание |
|----------|-----------|----------|
| `create` | `/pjm zone create <имя>` | Создать зону на текущей позиции |
| `delete` | `/pjm zone delete <имя>` | Удалить существующую зону |
| `list` | `/pjm zone list` | Показать список всех зон с координатами |
| `tp` | `/pjm zone tp <имя>` | Телепортироваться к указанной зоне |

**Примеры:**
```
/pjm zone create spawn_base
/pjm zone delete old_zone
/pjm zone list
/pjm zone tp spawn_base
```

---

## Контрольные точки

### `/pjm cp <действие>`
**Требует:** OP (уровень 2)

Система контрольных точек для режимов захвата территории.

| Действие | Синтаксис | Описание |
|----------|-----------|----------|
| `create` | `/pjm cp create <имя>` | Создать контрольную точку на текущей позиции |
| `delete` | `/pjm cp delete <имя>` | Удалить контрольную точку |
| `list` | `/pjm cp list` | Показать список всех точек с владельцами |
| `reset` | `/pjm cp reset [имя]` | Сбросить состояние точки (или всех точек) |
| `setowner` | `/pjm cp setowner <имя> <команда>` | Принудительно установить владельца точки |
| `tp` | `/pjm cp tp <имя>` | Телепортироваться к контрольной точке |

**Примеры:**
```
/pjm cp create point_alpha
/pjm cp setowner point_alpha team1
/pjm cp reset point_alpha
/pjm cp list
```

---

## Управление техникой

### `/pjm vehicle <действие>`
**Требует:** OP (уровень 2) или разрешения `Pjmbasemod.vehicle.spawn.*`

Система спавна и управления военной техникой.

| Действие | Синтаксис | Описание |
|----------|-----------|----------|
| `spawn` | `/pjm vehicle spawn <тип>` | Заспавнить технику указанного типа |
| `despawn` | `/pjm vehicle despawn [радиус]` | Удалить технику в радиусе (по умолчанию 10 блоков) |
| `point add` | `/pjm vehicle point add <имя>` | Создать точку автоспавна техники |
| `point remove` | `/pjm vehicle point remove <имя>` | Удалить точку спавна |
| `point list` | `/pjm vehicle point list` | Список всех точек спавна техники |

**Примеры:**
```
/pjm vehicle spawn tank
/pjm vehicle despawn 20
/pjm vehicle point add tank_spawn_1
/pjm vehicle point list
```

---

## Таймеры и объявления

### `/pjm timer <действие>`
**Требует:** OP (уровень 2) или разрешения `Pjmbasemod.timer.*`

Система таймеров для матчей и объявлений.

| Действие | Синтаксис | Описание |
|----------|-----------|----------|
| `start` | `/pjm timer start <секунды> [сообщение]` | Запустить обратный отсчёт |
| `stop` | `/pjm timer stop` | Остановить активный таймер |
| `broadcast` | `/pjm timer broadcast <сообщение>` | Отправить объявление всем игрокам |

**Функции таймера:**
- Автоматический обратный отсчёт с уведомлениями
- Настраиваемое сообщение по завершении
- Глобальные объявления с форматированием

**Примеры:**
```
/pjm timer start 300 Матч начинается!
/pjm timer stop
/pjm timer broadcast Подготовка к следующему раунду
```

---

## Управление китами

### `/pjm kit <действие>`
**Требует:** OP (уровень 2)

Система управления наборами экипировки (китами) для классов.

| Действие | Синтаксис | Описание |
|----------|-----------|----------|
| `list` | `/pjm kit list [класс] [команда]` | Показать доступные киты с фильтрацией |
| `reload` | `/pjm kit reload` | Перезагрузить конфигурацию китов |
| `give` | `/pjm kit give <игрок> <kitId>` | Принудительно выдать кит игроку |

**Примеры:**
```
/pjm kit list assault team1
/pjm kit reload
/pjm kit give Steve assault_basic
```

---

## Конфигурация

### `/pjm config <действие>`
**Требует:** OP (уровень 3)

Управление основной конфигурацией мода в реальном времени. Конфиг хранится в `config/pjmbasemod/wrb_config.json`.

| Действие | Синтаксис | Описание |
|----------|-----------|----------|
| `list` | `/pjm config list` | Показать все настройки по секциям |
| `get` | `/pjm config get <ключ>` | Получить текущее значение параметра |
| `set` | `/pjm config set <ключ> <значение>` | Установить значение (автосохранение) |
| `reload` | `/pjm config reload` | Перезагрузить конфиг с диска |

**Доступные ключи (с автодополнением):**

| Секция | Ключи |
|--------|-------|
| Teams | `teams.team1Name`, `teams.team2Name`, `teams.balanceThreshold` |
| Kit | `kitCooldownSeconds` |
| Capture | `capture.enabled`, `capture.captureTimeSeconds`, `capture.spawnCooldownSeconds`, `capture.defaultPointRadius` |
| AntiGrief | `antiGrief.enabled`, `antiGrief.maxDigDepth`, `antiGrief.preventItemDrop`, `antiGrief.preventItemPickup`, `antiGrief.preventBlockInteraction`, `antiGrief.enableBlockLogging` |
| MilSim | `milsim.disableHunger`, `milsim.disableArmor`, `milsim.blackDeathScreen`, `milsim.muteSoundsOnDeath`, `milsim.enableCameraHeadBob` |
| SquadHud | `squadHud.enableSquadPlayerList`, `squadHud.enableWeaponInfo`, `squadHud.enableItemSwitchPanel`, `squadHud.itemSwitchDisplayTime` |
| Chat | `chat.enabled`, `chat.localChatRadius`, `chat.defaultChatMode` |
| Debug | `enableDebugLogging` |

**Примеры:**
```
/pjm config list
/pjm config get teams.team1Name
/pjm config set antiGrief.enabled false
/pjm config set capture.captureTimeSeconds 90
/pjm config reload
```

---

## Отладка

### `/pjm debug <действие>`
**Требует:** OP (уровень 4) - только для владельцев сервера

Инструменты отладки и диагностики мода.

| Действие | Синтаксис | Описание |
|----------|-----------|----------|
| `sync` | `/pjm debug sync <игрок>` | Принудительно синхронизировать данные игрока |
| `netstat` | `/pjm debug netstat` | Показать статистику сетевых пакетов |
| `dump` | `/pjm debug dump` | Создать дамп текущего состояния мода |

**Примеры:**
```
/pjm debug sync Steve
/pjm debug netstat
/pjm debug dump
```

---

## Динамические измерения

### `/pjm dimension <действие>`
**Требует:** OP (уровень 2)

Создание, удаление и настройка динамических измерений в рантайме. Каждое измерение имеет свой конфиг в `config/pjmbasemod/dimensions/<имя>.json`.

| Действие | Синтаксис | Описание |
|----------|-----------|----------|
| `create` | `/pjm dimension create <имя> [void\|flat\|normal]` | Создать измерение (по умолчанию void) |
| `remove` | `/pjm dimension remove <имя>` | Удалить измерение (игроки перемещаются в overworld) |
| `list` | `/pjm dimension list` | Список всех динамических измерений |
| `tp` | `/pjm dimension tp <имя> [игрок]` | Телепортация в измерение (на спавн из конфига) |
| `info` | `/pjm dimension info` | Диагностика текущего измерения и состояния GameMode |
| `config` | `/pjm dimension config <имя>` | Показать все настройки измерения |
| `config set` | `/pjm dimension config <имя> set <ключ> <значение>` | Изменить настройку (автосохранение + применение) |
| `config reload` | `/pjm dimension config <имя> reload` | Перезагрузить конфиг с диска |

**Настройки конфига измерения (ключи для `config set`):**

| Группа | Ключи |
|--------|-------|
| Основные | `displayName` |
| Спавн | `spawnX`, `spawnY`, `spawnZ`, `spawnYaw` |
| Время/погода | `timeOfDay` (-1 = натуральное), `weather` (natural/clear/rain/storm), `timeFrozen` |
| Правила | `pvpEnabled`, `mobSpawning`, `allowBlockBreaking`, `allowBlockPlacing`, `allowExplosions`, `keepInventory`, `announceDeaths` |
| Анти-гриф | `antiGriefOverride` (global/enabled/disabled) |
| Граница мира | `worldBorderSize`, `worldBorderCenterX`, `worldBorderCenterZ` |
| Доступ | `requiredPermissionLevel`, `requiredTeam` |

**Типы генерации:**
- `void` — пустой мир (только воздух)
- `flat` — суперплоский (bedrock + dirt + grass)
- `normal` — копия генерации overworld

**Примеры:**
```
/pjm dimension create arena void
/pjm dimension config arena set displayName Arena PvP
/pjm dimension config arena set timeOfDay 6000
/pjm dimension config arena set weather clear
/pjm dimension config arena set pvpEnabled true
/pjm dimension config arena set mobSpawning false
/pjm dimension config arena set spawnY 65
/pjm dimension tp arena
/pjm dimension info
/pjm dimension config arena reload
/pjm dimension remove arena
```

---

## Матчевая система

### `/pjm match <действие>`
**Требует:** OP (уровень 2)

Управление матчами: старт, остановка, настройки, голосование за карты.

| Действие | Синтаксис | Описание |
|----------|-----------|----------|
| `start` | `/pjm match start` | Начать матч |
| `stop` | `/pjm match stop` | Остановить матч и вернуть в лобби |
| `status` | `/pjm match status` | Показать состояние матча |
| `settings` | `/pjm match settings` | Показать настройки матча |

**Автоматика:**
- Авто-старт при достижении минимума игроков
- Голосование за карту после матча
- Авто-баланс команд каждые 60 секунд
- Тикетная система (убийство = -1 тикет)
- Тикет-блид (контроль точек)

---

## Система чата

### `/chat <режим>`
**Требует:** Все игроки (уровень 0)

Переключение между режимами чата с различными областями видимости.

| Режим | Описание | Область действия |
|-------|----------|------------------|
| `local` | Локальный чат | Ограниченный радиус вокруг игрока |
| `global` | Глобальный чат | Весь сервер |
| `team` | Командный чат | Только участники команды |

**Дополнительные команды:**
- `/chat` (без аргументов) - показать текущий режим
- Автоматические подсказки доступных режимов

**Примеры:**
```
/chat team
/chat global
/chat local
/chat
```

---

## Справочная информация

### Доступные классы (classId)

| ID | Название | Описание |
|----|----------|----------|
| `assault` | Штурмовик | Основной боевой класс для атак |
| `machine_gunner` | Пулемётчик | Поддержка огнём, подавление |
| `medic` | Медик | Лечение и поддержка команды |
| `anti_tank` | Противотанковый | Борьба с бронетехникой |
| `engineer` | Инженер | Ремонт, строительство, разминирование |
| `sniper` | Снайпер | Дальний бой, разведка |
| `sso` | ССО (Спецназ) | Специальные операции (требует разрешение) |
| `uav_operator` | Оператор БПЛА | Управление дронами и разведка |

**Специальные классы:**
- `sso` - требует разрешение `Pjmbasemod.class.sso` (OP 2+)
- `spn` - требует разрешение `Pjmbasemod.class.spn` (OP 2+, только Team1)

### Доступные ранги (rankId)

| ID | Название | Уровень |
|----|----------|---------|
| `private` | Рядовой | 1 |
| `corporal` | Капрал | 2 |
| `sergeant` | Сержант | 3 |
| `staff_sergeant` | Старший сержант | 4 |
| `second_lieutenant` | Младший лейтенант | 5 |
| `lieutenant` | Лейтенант | 6 |
| `captain` | Капитан | 7 |
| `major` | Майор | 8 |
| `lieutenant_colonel` | Подполковник | 9 |
| `colonel` | Полковник | 10 |
| `major_general` | Генерал-майор | 11 |
| `colonel_general` | Генерал-полковник | 12 |

### Система разрешений

Мод поддерживает интеграцию с **LuckPerms** и использует уровни OP в качестве fallback.

#### Уровни доступа OP:
- **Уровень 0:** Обычные игроки - доступ к `/chat`
- **Уровень 1:** Базовые разрешения - изменение собственных данных
- **Уровень 2:** Модераторы - управление игроками и командами
- **Уровень 3:** Администраторы - конфигурация и настройка
- **Уровень 4:** Владельцы - отладка и диагностика

#### Основные разрешения LuckPerms:

**Базовые разрешения:**
- `Pjmbasemod.base` - Базовый доступ к моду
- `Pjmbasemod.team.join.self` - Смена собственной команды
- `Pjmbasemod.rank.set.self` - Изменение собственного ранга

**Управление другими игроками:**
- `Pjmbasemod.team.manage` - Управление командами
- `Pjmbasemod.team.join.other` - Назначение команд другим
- `Pjmbasemod.rank.manage` - Управление рангами других

**Административные разрешения:**
- `Pjmbasemod.class.zone.manage` - Управление зонами классов
- `Pjmbasemod.vehicle.spawn.manage` - Управление техникой
- `Pjmbasemod.vehicle.spawn.force` - Принудительный спавн техники
- `Pjmbasemod.timer.create` - Создание таймеров
- `Pjmbasemod.timer.manage` - Управление таймерами
- `Pjmbasemod.config.reload` - Перезагрузка конфигурации

**Интеграция с Minecraft:**
- `minecraft.command.team` - Доступ к `/team`
- `minecraft.command.scoreboard` - Доступ к `/scoreboard`

### Команды (teamId)

| ID | Название |
|----|----------|
| `team1` | Команда 1 (настраивается в конфиге) |
| `team2` | Команда 2 (настраивается в конфиге) |

### Технические особенности

**Архитектура команд:**
- Использует Brigadier (система команд Minecraft)
- Иерархическая структура с корневой командой `/pjm`
- Автоматическая регистрация через `@SubscribeEvent`
- Поддержка автодополнения и валидации аргументов

**Сетевая синхронизация:**
- Команды автоматически синхронизируют изменения с клиентом
- Использует пакеты `SyncPjmDataPacket` для обновления данных
- Поддержка принудительной синхронизации через `/pjm debug sync`

**Интернационализация:**
- Все сообщения используют `Component.translatable()`
- Поддержка множественных языков через файлы локализации
- Файлы локализации: `en_us.json`, `ru_ru.json`, `de_de.json`, и др.

**Конфигурация:**
- Горячая перезагрузка через `/pjm reload`
- Настройка в реальном времени через `/pjm config`
- Интеграция с системой конфигурации NeoForge

---

## Примеры использования

### Настройка матча
```bash
# Перемешать команды
/pjm team shuffle

# Проверить баланс
/pjm team balance

# Создать контрольные точки
/pjm cp create alpha
/pjm cp create bravo
/pjm cp create charlie

# Запустить таймер подготовки
/pjm timer start 60 Матч начинается через минуту!
```

### Управление игроком
```bash
# Полная настройка игрока
/pjm player Steve team team1
/pjm player Steve class assault
/pjm player Steve rank sergeant
/pjm player Steve points set 1500

# Принудительное открытие меню
/pjm player Steve openmenu class
```

### Настройка техники
```bash
# Создать точки спавна
/pjm vehicle point add tank_spawn_main
/pjm vehicle point add heli_spawn_base

# Заспавнить технику
/pjm vehicle spawn tank
/pjm vehicle spawn helicopter
```

### Отладка и диагностика
```bash
# Синхронизация данных
/pjm debug sync Steve

# Проверка сети
/pjm debug netstat

# Дамп состояния
/pjm debug dump
```

---

## Часто задаваемые вопросы

**Q: Как дать игроку доступ к специальным классам?**
A: Используйте LuckPerms: `/lp user <игрок> permission set Pjmbasemod.class.sso true`

**Q: Можно ли настроить названия команд?**
A: Да, через конфигурацию: `/pjm config set teams.team1Name vsrf` и `/pjm config set teams.team2Name nato`

**Q: Как сбросить все контрольные точки?**
A: Используйте `/pjm cp reset` без указания имени точки

**Q: Что делать, если команды не синхронизируются?**
A: Попробуйте `/pjm debug sync <игрок>` или `/pjm reload` для перезагрузки конфигурации

**Q: Как изменить порог дисбаланса команд?**
A: Используйте `/pjm config set teams.balanceThreshold 3` (по умолчанию 1)

**Q: Как создать арену в отдельном измерении?**
A: Создайте измерение и настройте конфиг:
```
/pjm dimension create arena void
/pjm dimension config arena set pvpEnabled true
/pjm dimension config arena set mobSpawning false
/pjm dimension config arena set weather clear
/pjm dimension config arena set timeOfDay 6000
```

**Q: Где хранятся файлы конфигурации?**
A: Все конфиги в `config/pjmbasemod/`:
- `wrb_config.json` — основной конфиг
- `wrb_kits.json` — конфигурация китов
- `match_settings.json` — настройки матчей
- `dynamic_dimensions.json` — реестр дименшонов
- `dimensions/<имя>.json` — конфиг каждого дименшона
- `match_history/` — логи матчей

---

*Документация актуальна для версии мода 1.21.1 NeoForge*
