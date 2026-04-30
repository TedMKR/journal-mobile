## макет карточки студента по поределенному предмету. предназначается для просмотра преподавателем

### на этой странице распологается:
- кнопка для возвращения к журналу
- фамилия, имя и отчество студента
- группа
- средняя успеваемость по предмету
- количество пропусков
- график посещаемости (по месяцам в семестре)
- progress bar сданных работ
- progress bar посещенных занятий (и практика, и лекция)

### сверху экрана должна быть кнопка для открытия меню, в котором кнопки для перехода на следующие страницы:
- главная ( teacher-home.md )
- личный кабинет ( teacher-dashbs.md )
- ведомости ( teacher-ved.md )

**CSS код из figma (desktop)**
```
/* Карточка студента - Desktop */

position: relative;
width: 1440px;
height: 1094px;

background: #EDEEED;


/* Электронный Журнал 1 */

position: absolute;
width: 200px;
height: 47px;
left: 70px;
top: 18px;

background: url(Электронный Журнал.png);


/* Rectangle 9 */

position: absolute;
width: 1440px;
height: 214px;
left: 0px;
top: 881px;

background: #223268;
border-radius: 20px 20px 0px 0px;


/* Шапка сайта */

position: absolute;
width: 339px;
height: 23px;
left: 340px;
top: 30px;



/* Личный кабинет */

position: absolute;
width: 179px;
height: 23px;
left: 340px;
top: 30px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 20px;
line-height: 24px;

color: #223268;



/* Ведомости */

position: absolute;
width: 120px;
height: 23px;
left: 559px;
top: 30px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 20px;
line-height: 24px;

color: #223268;



/* Dct ghtlvtns */

position: absolute;
width: 357px;
height: 26px;
left: 73px;
top: 88px;



/* Назад к журналу группы */

position: absolute;
width: 288px;
height: 26px;
left: 142px;
top: 88px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 22px;
line-height: 27px;

color: #A4A4A4;



/* Стрелка */

position: absolute;
left: 5.07%;
right: 92.22%;
top: 8.32%;
bottom: 89.93%;

transform: matrix(-1, 0, 0, 1, 0, 0);


/* a */

position: absolute;
left: 5.07%;
right: 92.22%;
top: 8.32%;
bottom: 89.93%;

transform: matrix(-1, 0, 0, 1, 0, 0);


/* Vector */

position: absolute;
left: 5.07%;
right: 92.22%;
top: 8.32%;
bottom: 89.93%;

background: #F47A5B;
transform: matrix(-1, 0, 0, 1, 0, 0);


/* Group */

position: absolute;
left: 5.07%;
right: 92.22%;
top: 8.32%;
bottom: 89.93%;

transform: matrix(-1, 0, 0, 1, 0, 0);


/* Vector */

position: absolute;
left: 5.07%;
right: 92.22%;
top: 8.32%;
bottom: 89.93%;

background: #A4A4A4;
transform: matrix(-1, 0, 0, 1, 0, 0);


/* Frame 3 */

position: absolute;
width: 1300px;
height: 173px;
left: 70px;
top: 136px;

border-radius: 20px;


/* Rectangle 45 */

position: absolute;
width: 1300px;
height: 238px;
left: 0px;
top: 0px;

background: #FFFFFF;
border-radius: 20px;


/* Петров Петр Петрович */

position: absolute;
width: 315px;
height: 30px;
left: 30px;
top: 20px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 700;
font-size: 25px;
line-height: 30px;

color: #223268;



/* Group 43 */

position: absolute;
width: 259px;
height: 36px;
left: 30px;
top: 60px;



/* Rectangle 46 */

position: absolute;
width: 259px;
height: 36px;
left: 30px;
top: 60px;

background: #E9EBF0;
border-radius: 10px;


/* 2022-ФГиИБ-ИСиТ-2б */

position: absolute;
width: 199px;
height: 20px;
left: 60px;
top: 68px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 18px;
line-height: 22px;

color: #223268;



/* Староста */

position: absolute;
width: 94px;
height: 22px;
left: 375px;
top: 24px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 20px;
line-height: 24px;

color: #A4A4A4;



/* Group 44 */

position: absolute;
width: 1631.07px;
height: 656.63px;
left: -267px;
top: -194px;



/* Vector 1 */

position: absolute;
width: 1617px;
height: 408px;
left: -267px;
top: -194px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 8 */

position: absolute;
width: 1617.26px;
height: 406.02px;
left: -266.95px;
top: -186.62px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 17 */

position: absolute;
width: 1617.52px;
height: 404.04px;
left: -266.91px;
top: -179.23px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 25 */

position: absolute;
width: 1617.78px;
height: 402.06px;
left: -266.86px;
top: -171.85px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 33 */

position: absolute;
width: 1618.04px;
height: 400.08px;
left: -266.82px;
top: -164.46px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 41 */

position: absolute;
width: 1618.3px;
height: 398.1px;
left: -266.77px;
top: -157.08px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 50 */

position: absolute;
width: 1618.56px;
height: 396.12px;
left: -266.73px;
top: -149.69px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 58 */

position: absolute;
width: 1618.82px;
height: 394.14px;
left: -266.68px;
top: -142.31px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 66 */

position: absolute;
width: 1619.08px;
height: 392.17px;
left: -266.64px;
top: -134.93px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 74 */

position: absolute;
width: 1619.34px;
height: 390.19px;
left: -266.59px;
top: -127.54px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 83 */

position: absolute;
width: 1619.6px;
height: 388.21px;
left: -266.55px;
top: -120.16px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 91 */

position: absolute;
width: 1619.86px;
height: 386.23px;
left: -266.5px;
top: -112.77px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 99 */

position: absolute;
width: 1620.12px;
height: 384.25px;
left: -266.45px;
top: -105.39px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 107 */

position: absolute;
width: 1620.38px;
height: 382.27px;
left: -266.41px;
top: -98px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 116 */

position: absolute;
width: 1620.64px;
height: 380.29px;
left: -266.36px;
top: -90.62px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 124 */

position: absolute;
width: 1620.9px;
height: 378.31px;
left: -266.32px;
top: -83.24px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 132 */

position: absolute;
width: 1621.17px;
height: 376.33px;
left: -266.27px;
top: -75.85px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 140 */

position: absolute;
width: 1621.43px;
height: 374.35px;
left: -266.23px;
top: -68.47px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 149 */

position: absolute;
width: 1621.69px;
height: 372.37px;
left: -266.18px;
top: -61.08px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 157 */

position: absolute;
width: 1621.95px;
height: 370.39px;
left: -266.14px;
top: -53.7px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 165 */

position: absolute;
width: 1622.21px;
height: 368.41px;
left: -266.09px;
top: -46.31px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 174 */

position: absolute;
width: 1622.47px;
height: 366.43px;
left: -266.05px;
top: -38.93px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 182 */

position: absolute;
width: 1622.73px;
height: 364.45px;
left: -266px;
top: -31.55px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 190 */

position: absolute;
width: 1622.99px;
height: 362.48px;
left: -265.95px;
top: -24.16px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 198 */

position: absolute;
width: 1623.25px;
height: 360.5px;
left: -265.91px;
top: -16.78px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 207 */

position: absolute;
width: 1623.51px;
height: 358.52px;
left: -265.86px;
top: -9.39px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 215 */

position: absolute;
width: 1623.77px;
height: 356.54px;
left: -265.82px;
top: -2.01px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 223 */

position: absolute;
width: 1624.03px;
height: 354.56px;
left: -265.77px;
top: 5.38px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 231 */

position: absolute;
width: 1624.29px;
height: 352.58px;
left: -265.73px;
top: 12.76px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 240 */

position: absolute;
width: 1624.55px;
height: 350.6px;
left: -265.68px;
top: 20.14px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 248 */

position: absolute;
width: 1624.81px;
height: 348.62px;
left: -265.64px;
top: 27.53px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 256 */

position: absolute;
width: 1625.07px;
height: 346.64px;
left: -265.59px;
top: 34.91px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 264 */

position: absolute;
width: 1625.33px;
height: 344.66px;
left: -265.55px;
top: 42.3px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 273 */

position: absolute;
width: 1625.59px;
height: 342.68px;
left: -265.5px;
top: 49.68px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 281 */

position: absolute;
width: 1625.85px;
height: 340.7px;
left: -265.45px;
top: 57.07px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 289 */

position: absolute;
width: 1626.11px;
height: 338.72px;
left: -265.41px;
top: 64.45px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 298 */

position: absolute;
width: 1626.37px;
height: 336.74px;
left: -265.36px;
top: 71.83px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 306 */

position: absolute;
width: 1626.63px;
height: 334.76px;
left: -265.32px;
top: 79.22px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 314 */

position: absolute;
width: 1626.89px;
height: 332.79px;
left: -265.27px;
top: 86.6px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 322 */

position: absolute;
width: 1627.15px;
height: 330.81px;
left: -265.23px;
top: 93.99px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 331 */

position: absolute;
width: 1627.41px;
height: 328.83px;
left: -265.18px;
top: 101.37px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 339 */

position: absolute;
width: 1627.67px;
height: 326.85px;
left: -265.14px;
top: 108.76px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 347 */

position: absolute;
width: 1627.93px;
height: 324.87px;
left: -265.09px;
top: 116.14px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 355 */

position: absolute;
width: 1628.19px;
height: 322.89px;
left: -265.05px;
top: 123.52px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 364 */

position: absolute;
width: 1628.45px;
height: 320.91px;
left: -265px;
top: 130.91px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 372 */

position: absolute;
width: 1628.71px;
height: 318.93px;
left: -264.95px;
top: 138.29px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Vector 1 - Vector 2 - 380 */

position: absolute;
width: 1628.98px;
height: 316.95px;
left: -264.91px;
top: 145.68px;

border: 1px solid rgba(34, 50, 104, 0.51);


/* Пропуски */

position: absolute;
width: 220px;
height: 113px;
left: 1050px;
top: 30px;



/* Rectangle 47 */

position: absolute;
width: 220px;
height: 113px;
left: 1050px;
top: 30px;

background: #FFFFFF;
border-radius: 10px;


/* Rectangle 48 */

position: absolute;
width: 220px;
height: 12px;
left: 1050px;
top: 30px;

background: #223268;
border-radius: 10px 10px 0px 0px;


/* Пропусков */

position: absolute;
width: 120px;
height: 23px;
left: 1060px;
top: 51px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 600;
font-size: 20px;
line-height: 24px;

color: #223268;



/* 2 */

position: absolute;
width: 20px;
height: 36px;
left: 1060px;
top: 80px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 600;
font-size: 35px;
line-height: 43px;

color: #223268;



/* Пропуски */

position: absolute;
width: 277px;
height: 113px;
left: 743px;
top: 30px;



/* Rectangle 47 */

position: absolute;
width: 277px;
height: 113px;
left: 743px;
top: 30px;

background: #FFFFFF;
border-radius: 10px;


/* Rectangle 48 */

position: absolute;
width: 277px;
height: 12px;
left: 743px;
top: 30px;

background: #223268;
border-radius: 10px 10px 0px 0px;


/* Средняя успеваемость */

position: absolute;
width: 250px;
height: 46px;
left: 753px;
top: 51px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 600;
font-size: 20px;
line-height: 24px;

color: #223268;



/* 4,5 */

position: absolute;
width: 55px;
height: 36px;
left: 753px;
top: 80px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 600;
font-size: 35px;
line-height: 43px;

color: #223268;



/* Rectangle 49 */

position: absolute;
width: 1300px;
height: 346px;
left: 70px;
top: 349px;

background: #FFFFFF;
border-radius: 20px;


/* Карточка посещаемости */

position: absolute;
width: 645px;
height: 286px;
left: 100px;
top: 379px;



/* Rectangle 37 */

position: absolute;
width: 645px;
height: 286px;
left: 100px;
top: 379px;

background: #E9EBF0;
border-radius: 20px;


/* Line 38 */

position: absolute;
width: 571px;
height: 0px;
left: 125px;
top: 619px;

border: 1px solid #223268;


/* Line 40 */

position: absolute;
width: 99.48px;
height: 0px;
left: 515px;
top: 485px;

border: 1px solid #223268;
transform: rotate(-30.17deg);


/* Посещаемость */

position: absolute;
width: 211px;
height: 27px;
left: 125px;
top: 399px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 700;
font-size: 22px;
line-height: 27px;
/* identical to box height */

color: #223268;



/* сентябрь */

position: absolute;
width: 107px;
height: 27px;
left: 144px;
top: 619px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 22px;
line-height: 27px;
/* identical to box height */

color: #223268;



/* октябрь */

position: absolute;
width: 93px;
height: 27px;
left: 301px;
top: 619px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 22px;
line-height: 27px;
/* identical to box height */

color: #223268;



/* ноябрь */

position: absolute;
width: 87px;
height: 23px;
left: 444px;
top: 621px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 22px;
line-height: 27px;

color: #223268;



/* декабрь */

position: absolute;
width: 96px;
height: 27px;
left: 581px;
top: 619px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 22px;
line-height: 27px;
/* identical to box height */

color: #223268;



/* График */

position: absolute;
width: 507px;
height: 98px;
left: 155px;
top: 449px;



/* Line 39 */

position: absolute;
width: 113.85px;
height: 0px;
left: 216px;
top: 464px;

border: 1px solid #223268;
transform: rotate(22.74deg);


/* Line 39 */

position: absolute;
width: 118.96px;
height: 0px;
left: 374px;
top: 506px;

border: 1px solid #223268;
transform: rotate(16.61deg);


/* Процент */

position: absolute;
width: 66px;
height: 29px;
left: 155px;
top: 449px;



/* Rectangle 39 */

position: absolute;
width: 66px;
height: 29px;
left: 155px;
top: 449px;

background: #223268;
border-radius: 15px;


/* 91% */

position: absolute;
width: 40px;
height: 24px;
left: 168px;
top: 451px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 22px;
line-height: 27px;

color: #FFFFFF;



/* Процент */

position: absolute;
width: 66px;
height: 29px;
left: 315px;
top: 493px;



/* Rectangle 39 */

position: absolute;
width: 66px;
height: 29px;
left: 315px;
top: 493px;

background: #223268;
border-radius: 15px;


/* 84% */

position: absolute;
width: 48px;
height: 24px;
left: 326px;
top: 494px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 22px;
line-height: 27px;

color: #FFFFFF;



/* Процент */

position: absolute;
width: 66px;
height: 29px;
left: 455px;
top: 518px;



/* Rectangle 39 */

position: absolute;
width: 66px;
height: 29px;
left: 455px;
top: 518px;

background: #223268;
border-radius: 15px;


/* 78% */

position: absolute;
width: 48px;
height: 24px;
left: 466px;
top: 519px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 22px;
line-height: 27px;

color: #FFFFFF;



/* Процент */

position: absolute;
width: 66px;
height: 29px;
left: 596px;
top: 472px;



/* Rectangle 39 */

position: absolute;
width: 66px;
height: 29px;
left: 596px;
top: 472px;

background: #223268;
border-radius: 15px;


/* 86% */

position: absolute;
width: 48px;
height: 24px;
left: 607px;
top: 473px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 400;
font-size: 22px;
line-height: 27px;

color: #FFFFFF;



/* Организация процессов разработки программного обеспечения */

position: absolute;
width: 710px;
height: 26px;
left: 662px;
top: 91px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 500;
font-size: 20px;
line-height: 24px;

color: #223268;



/* Group 46 */

position: absolute;
width: 555px;
height: 124px;
left: 785px;
top: 379px;



/* Rectangle 50 */

position: absolute;
width: 555px;
height: 124px;
left: 785px;
top: 379px;

background: #E9EBF0;
border-radius: 10px;


/* Сдано практик */

position: absolute;
width: 182px;
height: 27px;
left: 810px;
top: 399px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 700;
font-size: 22px;
line-height: 27px;
/* identical to box height */

color: #223268;



/* Group 45 */

position: absolute;
width: 505px;
height: 11px;
left: 810px;
top: 463px;



/* Rectangle 51 */

position: absolute;
width: 505px;
height: 11px;
left: 810px;
top: 463px;

background: #FFFFFF;
border-radius: 20px;


/* Rectangle 52 */

position: absolute;
width: 434px;
height: 11px;
left: 810px;
top: 463px;

background: #223268;
border-radius: 20px;


/* 6/7 */

position: absolute;
width: 41px;
height: 29px;
left: 1278px;
top: 427px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 700;
font-size: 22px;
line-height: 27px;

color: #223268;



/* Group 47 */

position: absolute;
width: 555px;
height: 124px;
left: 785px;
top: 541px;



/* Rectangle 50 */

position: absolute;
width: 555px;
height: 124px;
left: 785px;
top: 541px;

background: #E9EBF0;
border-radius: 10px;


/* Посещено занятий */

position: absolute;
width: 229px;
height: 27px;
left: 810px;
top: 561px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 700;
font-size: 22px;
line-height: 27px;
/* identical to box height */

color: #223268;



/* Group 45 */

position: absolute;
width: 505px;
height: 11px;
left: 810px;
top: 625px;



/* Rectangle 51 */

position: absolute;
width: 505px;
height: 11px;
left: 810px;
top: 625px;

background: #FFFFFF;
border-radius: 20px;


/* Rectangle 52 */

position: absolute;
width: 434px;
height: 11px;
left: 810px;
top: 625px;

background: #223268;
border-radius: 20px;


/* 14/16 */

position: absolute;
width: 57px;
height: 29px;
left: 1262px;
top: 589px;

font-family: 'Montserrat';
font-style: normal;
font-weight: 700;
font-size: 22px;
line-height: 27px;

color: #223268;


```