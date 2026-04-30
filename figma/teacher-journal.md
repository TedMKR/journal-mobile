## макет журнала предмета группы для преподавателя

### на этой странице распологается:
- еазвание предмета
- группа
- тип занятия 
- кнопка экспорта таблицы 
- таблица (журнал)
- кнопка редактирования таблицы

### таблица. таблица состоит из:
- строк с учениками группы
- столбцов с днями
- в ячейках указывается отметка ученику или отметка пропуска (н) в определенную дату. отметка ставится по пятибальной шкале (1, 2, 3, 4, 5). также преподаватель можнет написать комментарий 

### при нажатии на имя ученика в таблице должна открываться карточка студента ( teacher-journal-student-card.md )

### сверху экрана должна быть кнопка для открытия меню, в котором кнопки для перехода на следующие страницы:
- главная ( teacher-home.md )
- личный кабинет ( teacher-dashbs.md )
- ведомости ( teacher-ved.md )

**CSS код из figma**
```
/* Журнал - Phone */

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


/* Организация процессов разработки программного обеспечения */

position: absolute;
width: 368px;
height: 39px;
left: 10px;
top: 76px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 600;
font-size: 16px;
line-height: 20px;

color: #223268;



/* Практическое занятие */

position: absolute;
width: 181px;
height: 27px;
left: 10px;
top: 120px;



/* Rectangle 8 */

position: absolute;
width: 181px;
height: 27px;
left: 10px;
top: 120px;

background: #D3D3D3;
border-radius: 10px;


/* 2022-ФГиИБ-ИСиТ-2б */

position: absolute;
width: 155px;
height: 17px;
left: 23px;
top: 124px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 14px;
line-height: 17px;
/* identical to box height */

color: #223268;



/* Практическое занятие */

position: absolute;
width: 194px;
height: 27px;
left: 10px;
top: 152px;



/* Rectangle 8 */

position: absolute;
width: 194px;
height: 27px;
left: 10px;
top: 152px;

background: #D3D3D3;
border-radius: 10px;


/* Практические занятия */

position: absolute;
width: 167px;
height: 17px;
left: 23px;
top: 156px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 14px;
line-height: 17px;
/* identical to box height */

color: #223268;



/* таблица */

position: absolute;
width: 1300px;
height: 560px;
left: 10px;
top: 242px;



/* Rectangle 13 */

position: absolute;
width: 1300px;
height: 560px;
left: 10px;
top: 242px;

background: #FFFFFF;
border-radius: 10px;


/* Group 16 */

position: absolute;
width: 1300px;
height: 71px;
left: 10px;
top: 651px;



/* Line 5 */

position: absolute;
width: 1300px;
height: 0px;
left: 10px;
top: 722px;

border: 1px solid #223268;


/* Петров Петр Петрович */

position: absolute;
width: 193px;
height: 19px;
left: 68px;
top: 691px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #223268;



/* Line 4 */

position: absolute;
width: 1300px;
height: 0px;
left: 10px;
top: 682px;

border: 1px solid #223268;


/* Иванов Иван Иванович */

position: absolute;
width: 198px;
height: 18px;
left: 68px;
top: 651px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #223268;



/* Group 13 */

position: absolute;
width: 1300px;
height: 70px;
left: 10px;
top: 412px;



/* Line 5 */

position: absolute;
width: 1300px;
height: 0px;
left: 10px;
top: 482px;

border: 1px solid #223268;


/* Петров Петр Петрович */

position: absolute;
width: 193px;
height: 19px;
left: 68px;
top: 451px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #223268;



/* Line 4 */

position: absolute;
width: 1300px;
height: 0px;
left: 10px;
top: 442px;

border: 1px solid #223268;


/* Иванов Иван Иванович */

position: absolute;
width: 198px;
height: 20px;
left: 68px;
top: 412px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;
/* identical to box height */

color: #223268;



/* Rectangle 20 */

position: absolute;
width: 39px;
height: 482px;
left: 10px;
top: 320px;

background: #223268;
border-radius: 0px 0px 0px 10px;


/* 01 */

position: absolute;
width: 19px;
height: 18px;
left: 21px;
top: 331px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #FFFFFF;



/* 02 */

position: absolute;
width: 23px;
height: 18px;
left: 19px;
top: 371px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #FFFFFF;



/* 03 */

position: absolute;
width: 20px;
height: 18px;
left: 19px;
top: 411px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #FFFFFF;



/* 04 */

position: absolute;
width: 26px;
height: 18px;
left: 19px;
top: 451px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #FFFFFF;



/* 05 */

position: absolute;
width: 24px;
height: 18px;
left: 19px;
top: 491px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #FFFFFF;



/* 06 */

position: absolute;
width: 24px;
height: 18px;
left: 19px;
top: 531px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #FFFFFF;



/* 07 */

position: absolute;
width: 24px;
height: 18px;
left: 19px;
top: 571px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #FFFFFF;



/* 08 */

position: absolute;
width: 24px;
height: 18px;
left: 19px;
top: 611px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #FFFFFF;



/* 09 */

position: absolute;
width: 24px;
height: 18px;
left: 19px;
top: 651px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #FFFFFF;



/* 10 */

position: absolute;
width: 19px;
height: 18px;
left: 22px;
top: 691px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #FFFFFF;



/* 12 */

position: absolute;
width: 18px;
height: 18px;
left: 22px;
top: 771px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #FFFFFF;



/* 11 */

position: absolute;
width: 14px;
height: 18px;
left: 24px;
top: 731px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #FFFFFF;



/* Line 8 */

position: absolute;
width: 560px;
height: 0px;
left: 49px;
top: 242px;

border: 1px solid #223268;
transform: rotate(90deg);


/* Group 12 */

position: absolute;
width: 1300px;
height: 71px;
left: 10px;
top: 331px;



/* Line 5 */

position: absolute;
width: 1300px;
height: 0px;
left: 10px;
top: 402px;

border: 1px solid #223268;


/* Петров Петр Петрович */

position: absolute;
width: 193px;
height: 20px;
left: 68px;
top: 371px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;
/* identical to box height */

color: #223268;



/* Line 4 */

position: absolute;
width: 1300px;
height: 0px;
left: 10px;
top: 362px;

border: 1px solid #223268;


/* Иванов Иван Иванович */

position: absolute;
width: 198px;
height: 20px;
left: 68px;
top: 331px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;
/* identical to box height */

color: #223268;



/* Group 17 */

position: absolute;
width: 1300px;
height: 60px;
left: 10px;
top: 731px;



/* Петров Петр Петрович */

position: absolute;
width: 193px;
height: 20px;
left: 68px;
top: 771px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;
/* identical to box height */

color: #223268;



/* Line 4 */

position: absolute;
width: 1300px;
height: 0px;
left: 10px;
top: 762px;

border: 1px solid #223268;


/* Иванов Иван Иванович */

position: absolute;
width: 198px;
height: 20px;
left: 68px;
top: 731px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;
/* identical to box height */

color: #223268;



/* Group 15 */

position: absolute;
width: 1300px;
height: 71px;
left: 10px;
top: 571px;



/* Line 5 */

position: absolute;
width: 1300px;
height: 0px;
left: 10px;
top: 642px;

border: 1px solid #223268;


/* Петров Петр Петрович */

position: absolute;
width: 193px;
height: 20px;
left: 68px;
top: 611px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;
/* identical to box height */

color: #223268;



/* Line 4 */

position: absolute;
width: 1300px;
height: 0px;
left: 10px;
top: 602px;

border: 1px solid #223268;


/* Иванов Иван Иванович */

position: absolute;
width: 198px;
height: 20px;
left: 68px;
top: 571px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;
/* identical to box height */

color: #223268;



/* Group 14 */

position: absolute;
width: 1300px;
height: 70px;
left: 10px;
top: 492px;



/* Line 5 */

position: absolute;
width: 1300px;
height: 0px;
left: 10px;
top: 562px;

border: 1px solid #223268;


/* Петров Петр Петрович */

position: absolute;
width: 193px;
height: 20px;
left: 68px;
top: 532px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;
/* identical to box height */

color: #223268;



/* Line 4 */

position: absolute;
width: 1300px;
height: 0px;
left: 10px;
top: 522px;

border: 1px solid #223268;


/* Иванов Иван Иванович */

position: absolute;
width: 198px;
height: 20px;
left: 68px;
top: 492px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;
/* identical to box height */

color: #223268;



/* Rectangle 14 */

position: absolute;
width: 409px;
height: 80px;
left: 10px;
top: 242px;

background: #223268;
border-radius: 10px 10px 0px 0px;


/* Line 9 */

position: absolute;
width: 80px;
height: 0px;
left: 51px;
top: 242px;

border: 1px solid #FFFFFF;
transform: rotate(-90deg);


/* Line 12 */

position: absolute;
width: 96px;
height: 0px;
left: 298px;
top: 282px;

border: 1px solid #FFFFFF;


/* Group 11 */

position: absolute;
width: 410px;
height: 560px;
left: 299px;
top: 242px;



/* Line 18 */

position: absolute;
width: 560px;
height: 0px;
left: 709px;
top: 242px;

border: 1px solid #223268;
transform: rotate(90deg);


/* Line 16 */

position: absolute;
width: 518px;
height: 0px;
left: 629px;
top: 284px;

border: 1px solid #223268;
transform: rotate(90deg);


/* Сентябрь */

position: absolute;
width: 200px;
height: 80px;
left: 299px;
top: 242px;



/* Line 10 */

position: absolute;
width: 80px;
height: 0px;
left: 299px;
top: 242px;

border: 1px solid #FFFFFF;
transform: rotate(-90deg);


/* сентябрь */

position: absolute;
width: 90px;
height: 23px;
left: 349px;
top: 250px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #FFFFFF;



/* 01 */

position: absolute;
width: 19px;
height: 18px;
left: 309px;
top: 291px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #FFFFFF;



/* День */

position: absolute;
width: 40px;
height: 40px;
left: 339px;
top: 282px;



/* Line 11 */

position: absolute;
width: 40px;
height: 0px;
left: 339px;
top: 282px;

border: 1px solid #FFFFFF;
transform: rotate(-90deg);


/* 05 */

position: absolute;
width: 24px;
height: 18px;
left: 347px;
top: 291px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #FFFFFF;



/* Line 13 */

position: absolute;
width: 40px;
height: 0px;
left: 379px;
top: 282px;

border: 1px solid #FFFFFF;
transform: rotate(-90deg);


/* День */

position: absolute;
width: 40px;
height: 40px;
left: 379px;
top: 282px;



/* Line 11 */

position: absolute;
width: 40px;
height: 0px;
left: 379px;
top: 282px;

border: 1px solid #FFFFFF;
transform: rotate(-90deg);


/* 12 */

position: absolute;
width: 17px;
height: 18px;
left: 390px;
top: 291px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #FFFFFF;



/* Line 13 */

position: absolute;
width: 40px;
height: 0px;
left: 419px;
top: 282px;

border: 1px solid #FFFFFF;
transform: rotate(-90deg);


/* День */

position: absolute;
width: 40px;
height: 40px;
left: 419px;
top: 282px;



/* Line 11 */

position: absolute;
width: 40px;
height: 0px;
left: 419px;
top: 282px;

border: 1px solid #FFFFFF;
transform: rotate(-90deg);


/* 19 */

position: absolute;
width: 18px;
height: 18px;
left: 430px;
top: 291px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #FFFFFF;



/* Line 13 */

position: absolute;
width: 40px;
height: 0px;
left: 459px;
top: 282px;

border: 1px solid #FFFFFF;
transform: rotate(-90deg);


/* День */

position: absolute;
width: 40px;
height: 40px;
left: 459px;
top: 282px;



/* Line 11 */

position: absolute;
width: 40px;
height: 0px;
left: 459px;
top: 282px;

border: 1px solid #FFFFFF;
transform: rotate(-90deg);


/* 27 */

position: absolute;
width: 21px;
height: 18px;
left: 469px;
top: 291px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #FFFFFF;



/* Line 13 */

position: absolute;
width: 40px;
height: 0px;
left: 499px;
top: 282px;

border: 1px solid #FFFFFF;
transform: rotate(-90deg);


/* Студент */

position: absolute;
width: 77px;
height: 21px;
left: 119px;
top: 271px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #FFFFFF;



/* Vector 15 */

position: absolute;
width: 40px;
height: 0px;
left: 10px;
top: 402px;

border: 1px solid #FFFFFF;


/* Vector 16 */

position: absolute;
width: 40px;
height: 0px;
left: 10px;
top: 442px;

border: 1px solid #FFFFFF;


/* Vector 17 */

position: absolute;
width: 40px;
height: 0px;
left: 10px;
top: 482px;

border: 1px solid #FFFFFF;


/* Vector 18 */

position: absolute;
width: 40px;
height: 0px;
left: 10px;
top: 522px;

border: 1px solid #FFFFFF;


/* Vector 19 */

position: absolute;
width: 40px;
height: 0px;
left: 10px;
top: 562px;

border: 1px solid #FFFFFF;


/* Vector 20 */

position: absolute;
width: 40px;
height: 0px;
left: 10px;
top: 602px;

border: 1px solid #FFFFFF;


/* Vector 21 */

position: absolute;
width: 40px;
height: 0px;
left: 10px;
top: 642px;

border: 1px solid #FFFFFF;


/* Vector 22 */

position: absolute;
width: 40px;
height: 0px;
left: 10px;
top: 682px;

border: 1px solid #FFFFFF;


/* Vector 23 */

position: absolute;
width: 40px;
height: 0px;
left: 10px;
top: 722px;

border: 1px solid #FFFFFF;


/* Vector 24 */

position: absolute;
width: 40px;
height: 0px;
left: 10px;
top: 762px;

border: 1px solid #FFFFFF;


/* Line 7 */

position: absolute;
width: 480px;
height: 0px;
left: 298px;
top: 321px;

border: 1px solid #223268;
transform: rotate(90deg);


/* Line 9 */

position: absolute;
width: 480px;
height: 0px;
left: 378px;
top: 321px;

border: 1px solid #223268;
transform: rotate(90deg);


/* Line 8 */

position: absolute;
width: 480px;
height: 0px;
left: 338px;
top: 321px;

border: 1px solid #223268;
transform: rotate(90deg);


/* Group 18 */

position: absolute;
width: 13px;
height: 221px;
left: 312px;
top: 329px;



/* н */

position: absolute;
width: 12px;
height: 21px;
left: 312px;
top: 450px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* н */

position: absolute;
width: 12px;
height: 21px;
left: 312px;
top: 529px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* 5 */

position: absolute;
width: 10px;
height: 21px;
left: 313px;
top: 371px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* 4 */

position: absolute;
width: 13px;
height: 21px;
left: 312px;
top: 489px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* н */

position: absolute;
width: 12px;
height: 21px;
left: 312px;
top: 329px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* н */

position: absolute;
width: 12px;
height: 21px;
left: 312px;
top: 411px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* Group 19 */

position: absolute;
width: 13px;
height: 221px;
left: 312px;
top: 569px;



/* н */

position: absolute;
width: 12px;
height: 21px;
left: 312px;
top: 690px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* н */

position: absolute;
width: 12px;
height: 21px;
left: 312px;
top: 769px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* 5 */

position: absolute;
width: 10px;
height: 21px;
left: 313px;
top: 611px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* 4 */

position: absolute;
width: 13px;
height: 21px;
left: 312px;
top: 729px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* н */

position: absolute;
width: 12px;
height: 21px;
left: 312px;
top: 569px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* н */

position: absolute;
width: 12px;
height: 21px;
left: 312px;
top: 651px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* Group 18 */

position: absolute;
width: 13px;
height: 221px;
left: 353px;
top: 329px;



/* н */

position: absolute;
width: 12px;
height: 21px;
left: 353px;
top: 450px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* н */

position: absolute;
width: 12px;
height: 21px;
left: 353px;
top: 529px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* 5 */

position: absolute;
width: 10px;
height: 21px;
left: 354px;
top: 371px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* 4 */

position: absolute;
width: 13px;
height: 21px;
left: 353px;
top: 489px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* н */

position: absolute;
width: 12px;
height: 21px;
left: 353px;
top: 329px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* н */

position: absolute;
width: 12px;
height: 21px;
left: 353px;
top: 411px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* Group 19 */

position: absolute;
width: 13px;
height: 221px;
left: 353px;
top: 569px;



/* н */

position: absolute;
width: 12px;
height: 21px;
left: 353px;
top: 690px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* н */

position: absolute;
width: 12px;
height: 21px;
left: 353px;
top: 769px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* 5 */

position: absolute;
width: 10px;
height: 21px;
left: 354px;
top: 611px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* 4 */

position: absolute;
width: 13px;
height: 21px;
left: 353px;
top: 729px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* н */

position: absolute;
width: 12px;
height: 21px;
left: 353px;
top: 569px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* н */

position: absolute;
width: 12px;
height: 21px;
left: 353px;
top: 651px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* Line 37 */

position: absolute;
width: 40px;
height: 0px;
left: 10px;
top: 321px;

border: 1px solid #FFFFFF;
transform: rotate(180deg);


/* Vector 14 */

position: absolute;
width: 40px;
height: 0px;
left: 10px;
top: 361px;

border: 1px solid #FFFFFF;


/* Кнопка "Редактировать" */

position: absolute;
width: 221px;
height: 37px;
left: 86px;
top: 839px;



/* Rectangle 19 */

position: absolute;
width: 221px;
height: 37px;
left: 86px;
top: 839px;

background: #223268;
border-radius: 10px;


/* Редактировать */

position: absolute;
width: 141px;
height: 24px;
left: 126px;
top: 846px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #FFFFFF;



/* Rectangle 21 */

position: absolute;
width: 383px;
height: 12px;
left: 10px;
top: 807px;

background: #FFFFFF;
border-radius: 10px 0px 0px 10px;


/* Rectangle 22 */

position: absolute;
width: 127px;
height: 12px;
left: 20px;
top: 807px;

background: #D3D3D3;
border-radius: 10px;


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


/* Кнопка "Экспорт" */

position: absolute;
width: 168px;
height: 35px;
left: calc(50% - 168px/2 - 102.5px);
top: 193px;



/* Rectangle 19 */

position: absolute;
width: 168px;
height: 35px;
left: 10px;
top: 193px;

background: #223268;
border-radius: 10px;


/* Экспорт */

position: absolute;
width: 70px;
height: 24px;
left: 78px;
top: 199px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #FFFFFF;



/* System Update Download - iconSvg.co */

position: absolute;
width: 23px;
height: 23px;
left: 40px;
top: 199px;



/* Group */

position: absolute;
left: 6.95%;
right: 6.95%;
top: 16.17%;
bottom: 16.17%;



/* Vector */

position: absolute;
left: 38.81%;
right: 38.81%;
top: 16.17%;
bottom: 34.09%;

background: #FFFFFF;


/* Vector */

position: absolute;
left: 6.95%;
right: 6.95%;
top: 16.17%;
bottom: 16.17%;

background: #FFFFFF;
```

### состояние ячейки при выборе отметки/пропуска/комментария в ячейке (для определенного ученика)
```
/* Rectangle 23 */

box-sizing: border-box;

position: absolute;
width: 226px;
height: 150px;
left: 605px;
top: 327px;

background: #FFFFFF;
border: 1px solid #223268;
box-shadow: -4px 4px 4px rgba(0, 0, 0, 0.25);
border-radius: 10px;


/* Кнопка "Сохранить" */

position: absolute;
width: 119px;
height: 27px;
left: calc(50% - 119px/2 + 41.5px);
top: 440px;



/* Rectangle 19 */

position: absolute;
width: 119px;
height: 27px;
left: 702px;
top: 440px;

background: #223268;
border-radius: 4px;


/* Сохранить */

position: absolute;
width: 78px;
height: 17px;
left: 722px;
top: 443px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 14px;
line-height: 17px;
/* identical to box height */

color: #FFFFFF;



/* Group 38 */

position: absolute;
width: 26px;
height: 21px;
left: 795px;
top: 337px;



/* Rectangle 30 */

position: absolute;
width: 26px;
height: 21px;
left: 795px;
top: 337px;

background: #DBDEE7;
border-radius: 4px;


/* 5 */

position: absolute;
width: 10px;
height: 18px;
left: 803px;
top: 337px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #223268;



/* Group 37 */

position: absolute;
width: 26px;
height: 21px;
left: 759px;
top: 337px;

filter: drop-shadow(-2px 2px 4px rgba(211, 211, 211, 0.7));


/* Rectangle 29 */

box-sizing: border-box;

position: absolute;
width: 26px;
height: 21px;
left: 759px;
top: 337px;

background: #C3C8D6;
border: 0.5px solid #223268;
border-radius: 4px;


/* 4 */

position: absolute;
width: 11px;
height: 18px;
left: 766px;
top: 337px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #223268;



/* Group 36 */

position: absolute;
width: 26px;
height: 21px;
left: 723px;
top: 337px;



/* Rectangle 28 */

position: absolute;
width: 26px;
height: 21px;
left: 723px;
top: 337px;

background: #DBDEE7;
border-radius: 4px;


/* 3 */

position: absolute;
width: 10px;
height: 18px;
left: 731px;
top: 337px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #223268;



/* Group 34 */

position: absolute;
width: 26px;
height: 24px;
left: 651px;
top: 334px;



/* Rectangle 26 */

position: absolute;
width: 26px;
height: 21px;
left: 651px;
top: 337px;

background: #DBDEE7;
border-radius: 4px;


/* у */

position: absolute;
width: 9px;
height: 20px;
left: 660px;
top: 334px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;
/* identical to box height */

color: #223268;



/* Group 33 */

position: absolute;
width: 26px;
height: 22px;
left: 615px;
top: 336px;



/* Rectangle 25 */

position: absolute;
width: 26px;
height: 21px;
left: 615px;
top: 337px;

background: #DBDEE7;
border-radius: 4px;


/* н */

position: absolute;
width: 11px;
height: 18px;
left: 622px;
top: 336px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #223268;



/* Group 35 */

position: absolute;
width: 26px;
height: 22px;
left: 687px;
top: 336px;



/* Rectangle 27 */

position: absolute;
width: 26px;
height: 21px;
left: 687px;
top: 337px;

background: #DBDEE7;
border-radius: 4px;


/* 2 */

position: absolute;
width: 10px;
height: 18px;
left: 695px;
top: 336px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 16px;
line-height: 20px;

color: #223268;



/* Group 39 */

position: absolute;
width: 206px;
height: 57px;
left: 615px;
top: 368px;



/* Rectangle 31 */

position: absolute;
width: 206px;
height: 57px;
left: 615px;
top: 368px;

background: #EDEEED;
border-radius: 4px;


/* Добавьте комментарий... */

position: absolute;
width: 157px;
height: 15px;
left: 624px;
top: 375px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 12px;
line-height: 15px;
/* identical to box height */

color: #BBBBBB;
```