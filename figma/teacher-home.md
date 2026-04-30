## макет главной страницы преподавателя

### по сути это расписание занятий для авторизованного преподавателя, где:
- день недели
- номер пары по счету (1, 2, 3...)
- название предмета
- тип занятия (лекция/практическое занятие)
- номер группы

### при надатии на карточку занятия должен открываться журнал группы по данному предмету ( teacher-journal.md )

### сверху экрана должна быть кнопка для открытия меню, в котором кнопки для перехода на следующие страницы:
- главная ( teacher-home.md )
- личный кабинет ( teacher-dashbs.md )
- ведомости ( teacher-ved.md )

**CSS код из figma**
```
/* Главная - Phone */

position: relative;
width: 393px;
height: 901px;

background: #EDEEED;


/* Электронный Журнал 1 */

position: absolute;
width: 132px;
height: 31px;
left: 9px;
top: 20px;

background: url(Электронный Журнал.png);


/* Иванов Иван Иванович */

position: absolute;
width: 202px;
height: 19px;
left: 10px;
top: 76px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 600;
font-size: 16px;
line-height: 20px;

color: #223268;



/* День */

position: absolute;
width: 373px;
height: 351px;
left: 10px;
top: 110px;



/* Rectangle 10 */

position: absolute;
width: 373px;
height: 351px;
left: 10px;
top: 110px;

background: #FFFFFF;
border-radius: 10px;


/* Понедельник */

position: absolute;
width: 120px;
height: 18px;
left: 20px;
top: 119px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 600;
font-size: 16px;
line-height: 20px;

color: #7E8E99;



/* Предмет */

position: absolute;
width: 353px;
height: 147px;
left: 20px;
top: 148px;



/* Rectangle 11 */

position: absolute;
width: 353px;
height: 147px;
left: 20px;
top: 148px;

background: #EDEEED;
border-radius: 7px;


/* Rectangle 12 */

position: absolute;
width: 353px;
height: 147px;
left: 20px;
top: 148px;

background: #EDEEED;
border-radius: 7px;


/* Организация процессов разработки программного обеспечения */

position: absolute;
width: 276px;
height: 33px;
left: 32px;
top: 184px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 500;
font-size: 14px;
line-height: 17px;

color: #223268;



/* Цифра */

position: absolute;
width: 20px;
height: 21px;
left: 32px;
top: 158px;



/* Rectangle 7 */

position: absolute;
width: 20px;
height: 20px;
left: 32px;
top: 159px;

background: #D3D3D3;
border-radius: 7px;


/* 1 */

position: absolute;
width: 6px;
height: 16px;
left: 39px;
top: 158px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 600;
font-size: 16px;
line-height: 20px;

color: #223268;



/* 9:00 - 10:30 */

position: absolute;
width: 79px;
height: 16px;
left: 61px;
top: 160px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 14px;
line-height: 17px;

color: #223268;



/* Практическое занятие */

position: absolute;
width: 197px;
height: 25px;
left: 52px;
top: 227px;



/* Rectangle 8 */

position: absolute;
width: 197px;
height: 25px;
left: 52px;
top: 227px;

background: #D3D3D3;
border-radius: 7px;


/* Практическое занятие */

position: absolute;
width: 167px;
height: 18px;
left: 67px;
top: 230px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 14px;
line-height: 17px;

color: #223268;



/* Практическое занятие */

position: absolute;
width: 186px;
height: 25px;
left: 52px;
top: 257px;



/* Rectangle 8 */

position: absolute;
width: 186px;
height: 25px;
left: 52px;
top: 257px;

background: #D3D3D3;
border-radius: 7px;


/* 2022-ФГиИБ-ИСиТ-2б */

position: absolute;
width: 156px;
height: 18px;
left: 67px;
top: 260px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 14px;
line-height: 17px;

color: #223268;



/* Стрелка */

position: absolute;
height: 11.59px;
left: 85.75%;
right: 8.25%;
top: calc(50% - 11.59px/2 - 174.7px);



/* a */

position: absolute;
left: 85.75%;
right: 8.25%;
top: 29.97%;
bottom: 68.75%;



/* Vector */

position: absolute;
left: 85.75%;
right: 8.25%;
top: 29.97%;
bottom: 68.75%;

background: #F47A5B;


/* Group */

position: absolute;
left: 85.75%;
right: 8.25%;
top: 29.97%;
bottom: 68.75%;



/* Vector */

position: absolute;
left: 85.75%;
right: 8.25%;
top: 29.97%;
bottom: 68.75%;

background: #223268;


/* Предмет */

position: absolute;
width: 353px;
height: 147px;
left: 20px;
top: 304px;



/* Предмет */

position: absolute;
width: 353px;
height: 147px;
left: 20px;
top: 304px;



/* Rectangle 11 */

position: absolute;
width: 353px;
height: 147px;
left: 20px;
top: 304px;

background: rgba(34, 50, 104, 0.1);
border-radius: 7px;


/* Объектно-ориентированное программирование */

position: absolute;
width: 276px;
height: 33px;
left: 32px;
top: 342px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 500;
font-size: 14px;
line-height: 17px;

color: #223268;



/* Цифра */

position: absolute;
width: 20px;
height: 21px;
left: 32px;
top: 315px;



/* Rectangle 7 */

position: absolute;
width: 20px;
height: 20px;
left: 32px;
top: 316px;

background: rgba(34, 50, 104, 0.28);
border-radius: 7px;


/* 1 */

position: absolute;
width: 6px;
height: 16px;
left: 39px;
top: 315px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 600;
font-size: 16px;
line-height: 20px;

color: #223268;



/* 9:00 - 10:30 */

position: absolute;
width: 79px;
height: 16px;
left: 61px;
top: 317px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 14px;
line-height: 17px;

color: #223268;



/* Практическое занятие */

position: absolute;
width: 197px;
height: 25px;
left: 52px;
top: 384px;



/* Rectangle 8 */

position: absolute;
width: 197px;
height: 25px;
left: 52px;
top: 384px;

background: rgba(34, 50, 104, 0.28);
border-radius: 7px;


/* Практическое занятие */

position: absolute;
width: 167px;
height: 18px;
left: 67px;
top: 387px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 14px;
line-height: 17px;

color: #223268;



/* Практическое занятие */

position: absolute;
width: 170px;
height: 25px;
left: 52px;
top: 414px;



/* Rectangle 8 */

position: absolute;
width: 170px;
height: 25px;
left: 52px;
top: 414px;

background: rgba(34, 50, 104, 0.28);
border-radius: 7px;


/* 2024-ФГиИБ-ИС-2б */

position: absolute;
width: 140px;
height: 18px;
left: 67px;
top: 417px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 14px;
line-height: 17px;

color: #223268;



/* Стрелка */

position: absolute;
height: 11.59px;
left: 85.75%;
right: 8.25%;
top: calc(50% - 11.59px/2 - 18.7px);



/* a */

position: absolute;
left: 85.75%;
right: 8.25%;
top: 47.28%;
bottom: 51.43%;



/* Vector */

position: absolute;
left: 85.75%;
right: 8.25%;
top: 47.28%;
bottom: 51.43%;

background: #F47A5B;


/* Group */

position: absolute;
left: 85.75%;
right: 8.25%;
top: 47.28%;
bottom: 51.43%;



/* Vector */

position: absolute;
left: 85.75%;
right: 8.25%;
top: 47.28%;
bottom: 51.43%;

background: #223268;


/* День */

position: absolute;
width: 373px;
height: 351px;
left: 10px;
top: 476px;



/* Rectangle 10 */

position: absolute;
width: 373px;
height: 351px;
left: 10px;
top: 476px;

background: #FFFFFF;
border-radius: 10px;


/* Вторник */

position: absolute;
width: 88px;
height: 18px;
left: 20px;
top: 485px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 600;
font-size: 16px;
line-height: 20px;

color: #7E8E99;



/* Предмет */

position: absolute;
width: 353px;
height: 147px;
left: 20px;
top: 513px;



/* Rectangle 11 */

position: absolute;
width: 353px;
height: 147px;
left: 20px;
top: 513px;

background: #EDEEED;
border-radius: 7px;


/* Организация процессов разработки программного обеспечения */

position: absolute;
width: 276px;
height: 33px;
left: 32px;
top: 550px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 500;
font-size: 14px;
line-height: 17px;

color: #223268;



/* Цифра */

position: absolute;
width: 20px;
height: 21px;
left: 32px;
top: 524px;



/* Rectangle 7 */

position: absolute;
width: 20px;
height: 20px;
left: 32px;
top: 525px;

background: #D3D3D3;
border-radius: 7px;


/* 1 */

position: absolute;
width: 6px;
height: 16px;
left: 39px;
top: 524px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 600;
font-size: 16px;
line-height: 20px;

color: #223268;



/* 9:00 - 10:30 */

position: absolute;
width: 79px;
height: 16px;
left: 61px;
top: 526px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 14px;
line-height: 17px;

color: #223268;



/* Практическое занятие */

position: absolute;
width: 197px;
height: 25px;
left: 52px;
top: 593px;



/* Rectangle 8 */

position: absolute;
width: 197px;
height: 25px;
left: 52px;
top: 593px;

background: #D3D3D3;
border-radius: 7px;


/* Практическое занятие */

position: absolute;
width: 167px;
height: 18px;
left: 67px;
top: 596px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 14px;
line-height: 17px;

color: #223268;



/* Практическое занятие */

position: absolute;
width: 186px;
height: 25px;
left: 52px;
top: 623px;



/* Rectangle 8 */

position: absolute;
width: 186px;
height: 25px;
left: 52px;
top: 623px;

background: #D3D3D3;
border-radius: 7px;


/* 2022-ФГиИБ-ИСиТ-2б */

position: absolute;
width: 156px;
height: 18px;
left: 67px;
top: 626px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 14px;
line-height: 17px;

color: #223268;



/* Стрелка */

position: absolute;
height: 11.59px;
left: 85.75%;
right: 8.25%;
top: calc(50% - 11.59px/2 + 190.3px);



/* a */

position: absolute;
left: 85.75%;
right: 8.25%;
top: 70.48%;
bottom: 28.24%;



/* Vector */

position: absolute;
left: 85.75%;
right: 8.25%;
top: 70.48%;
bottom: 28.24%;

background: #F47A5B;


/* Group */

position: absolute;
left: 85.75%;
right: 8.25%;
top: 70.48%;
bottom: 28.24%;



/* Vector */

position: absolute;
left: 85.75%;
right: 8.25%;
top: 70.48%;
bottom: 28.24%;

background: #223268;


/* Предмет */

position: absolute;
width: 353px;
height: 147px;
left: 20px;
top: 670px;



/* Предмет */

position: absolute;
width: 353px;
height: 147px;
left: 20px;
top: 670px;



/* Rectangle 11 */

position: absolute;
width: 353px;
height: 147px;
left: 20px;
top: 670px;

background: #EDEEED;
border-radius: 7px;


/* Объектно-ориентированное программирование */

position: absolute;
width: 276px;
height: 33px;
left: 32px;
top: 708px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 500;
font-size: 14px;
line-height: 17px;

color: #223268;



/* Цифра */

position: absolute;
width: 20px;
height: 21px;
left: 32px;
top: 681px;



/* Rectangle 7 */

position: absolute;
width: 20px;
height: 20px;
left: 32px;
top: 682px;

background: #D3D3D3;
border-radius: 7px;


/* 1 */

position: absolute;
width: 6px;
height: 16px;
left: 39px;
top: 681px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 600;
font-size: 16px;
line-height: 20px;

color: #223268;



/* 9:00 - 10:30 */

position: absolute;
width: 79px;
height: 16px;
left: 61px;
top: 683px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 14px;
line-height: 17px;

color: #223268;



/* Практическое занятие */

position: absolute;
width: 197px;
height: 25px;
left: 52px;
top: 750px;



/* Rectangle 8 */

position: absolute;
width: 197px;
height: 25px;
left: 52px;
top: 750px;

background: #D3D3D3;
border-radius: 7px;


/* Практическое занятие */

position: absolute;
width: 167px;
height: 18px;
left: 67px;
top: 753px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 14px;
line-height: 17px;

color: #223268;



/* Практическое занятие */

position: absolute;
width: 170px;
height: 25px;
left: 52px;
top: 780px;



/* Rectangle 8 */

position: absolute;
width: 170px;
height: 25px;
left: 52px;
top: 780px;

background: #D3D3D3;
border-radius: 7px;


/* 2024-ФГиИБ-ИС-2б */

position: absolute;
width: 140px;
height: 18px;
left: 67px;
top: 783px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 14px;
line-height: 17px;

color: #223268;



/* Стрелка */

position: absolute;
height: 11.59px;
left: 85.75%;
right: 8.25%;
top: calc(50% - 11.59px/2 + 347.3px);



/* a */

position: absolute;
left: 85.75%;
right: 8.25%;
top: 87.9%;
bottom: 10.81%;



/* Vector */

position: absolute;
left: 85.75%;
right: 8.25%;
top: 87.9%;
bottom: 10.81%;

background: #F47A5B;


/* Group */

position: absolute;
left: 85.75%;
right: 8.25%;
top: 87.9%;
bottom: 10.81%;



/* Vector */

position: absolute;
left: 85.75%;
right: 8.25%;
top: 87.9%;
bottom: 10.81%;

background: #223268;


/* День */

position: absolute;
width: 373px;
height: 68px;
left: 10px;
top: 842px;



/* Rectangle 10 */

position: absolute;
width: 373px;
height: 68px;
left: 10px;
top: 842px;

background: #FFFFFF;
border-radius: 10px;


/* Среда */

position: absolute;
width: 88px;
height: 18px;
left: 20px;
top: 851px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 600;
font-size: 16px;
line-height: 20px;

color: #7E8E99;



/* Нет занятий */

position: absolute;
width: 92px;
height: 16px;
left: 150px;
top: 879px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 500;
font-size: 14px;
line-height: 17px;

color: #223268;



/* Burger Restaurant - iconSvg.co */

position: absolute;
width: 24px;
height: 24px;
left: 359px;
top: 24px;



/* Group */

position: absolute;
left: 0.8%;
right: 0.8%;
top: 5.66%;
bottom: 5.66%;



/* Clip path group */

position: absolute;
left: 0.8%;
right: 0.8%;
top: 5.66%;
bottom: 84.38%;



/* c */

position: absolute;
left: 0.8%;
right: 0.8%;
top: 5.66%;
bottom: 84.38%;



/* Vector */

position: absolute;
left: 0.8%;
right: 0.8%;
top: 5.66%;
bottom: 84.38%;

background: #000000;


/* Group */

position: absolute;
left: 0.8%;
right: 0.8%;
top: 5.72%;
bottom: 84.44%;



/* Vector */

position: absolute;
left: 0.8%;
right: 0.8%;
top: 5.72%;
bottom: 84.44%;

background: #223268;


/* Clip path group */

position: absolute;
left: 0.8%;
right: 0.8%;
top: 44.92%;
bottom: 44.92%;



/* b */

position: absolute;
left: 0.8%;
right: 0.8%;
top: 44.92%;
bottom: 44.92%;



/* Vector */

position: absolute;
left: 0.8%;
right: 0.8%;
top: 44.92%;
bottom: 44.92%;

background: #000000;


/* Group */

position: absolute;
left: 0.8%;
right: 0.8%;
top: 45.08%;
bottom: 45.08%;



/* Vector */

position: absolute;
left: 0.8%;
right: 0.8%;
top: 45.08%;
bottom: 45.08%;

background: #223268;


/* Clip path group */

position: absolute;
left: 0.8%;
right: 0.8%;
top: 84.38%;
bottom: 5.66%;



/* a */

position: absolute;
left: 0.8%;
right: 0.8%;
top: 84.38%;
bottom: 5.66%;



/* Vector */

position: absolute;
left: 0.8%;
right: 0.8%;
top: 84.38%;
bottom: 5.66%;

background: #000000;


/* Group */

position: absolute;
left: 0.8%;
right: 0.8%;
top: 84.44%;
bottom: 5.72%;



/* Vector */

position: absolute;
left: 0.8%;
right: 0.8%;
top: 84.44%;
bottom: 5.72%;

background: #223268;

```