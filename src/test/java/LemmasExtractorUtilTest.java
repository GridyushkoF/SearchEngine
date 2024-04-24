import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import searchengine.util.LemmaExtractorCacheProxy;
import searchengine.util.LemmasExtractorUtil;

import java.util.HashMap;

public class LemmasExtractorUtilTest {

    public LemmasExtractorUtilTest() {
    }
    private final LemmaExtractorCacheProxy extractorProxy = new LemmaExtractorCacheProxy();
    private final LemmasExtractorUtil extractor = new LemmasExtractorUtil(extractorProxy);
    @Test
    public void getWordNormalFormTest() {
        String word1 = "цвета.";
        String expected1 = "цвет";
        String actual1 = extractorProxy.getWordNormalForm(word1);
        String word2 = "12,";
        String expected2 = "12";
        String actual2 = extractorProxy.getWordNormalForm(word2);
        String word3 = "заусенцы,";
        String expected3 = "заусенец";
        String actual3 = extractorProxy.getWordNormalForm(word3);
        Assertions.assertEquals(expected1,actual1);
        Assertions.assertEquals(expected2,actual2);
        Assertions.assertEquals(expected3,actual3);

    }
    @Test
    public void getLemmas2rankingTest() {
        String html = """
                <!DOCTYPE html>
                <!-- saved from url=(0042)https://www.playback.ru/catalog/1310.html/ -->
                <html m_init="2210590202403151151"><head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
                <title>Купить чехлы для Huawei/Honor</title>
                <meta name="description" content="купить чехлы для Huawei/Honor, купить самовывозом чехлы для Huawei/Honor">
                <meta name="keywords" content="Чехлы для Huawei/Honor, купитьчехлы для Huawei/Honor">
                                
                <meta http-equiv="Last-Modified" content="Mon, 14 Feb 2022 22:01:52 GMT">
                <link rel="shortcut icon" href="https://www.playback.ru/favicon.ico">
                <link rel="apple-touch-icon" href="https://www.playback.ru/logo_apple.png">
                <link rel="StyleSheet" href="./Купить чехлы для Huawei_Honor_files/styles.css" type="text/css" media="all">
                	<link rel="stylesheet" href="./Купить чехлы для Huawei_Honor_files/jquery-ui.css">
                	<script type="text/javascript" async="" src="./Купить чехлы для Huawei_Honor_files/8LKJc6dMce"></script><script async="" src="./Купить чехлы для Huawei_Honor_files/tag.js.Без названия"></script><script src="./Купить чехлы для Huawei_Honor_files/jquery-1.8.3.js.Без названия"></script>
                	<script src="./Купить чехлы для Huawei_Honor_files/jquery-ui.js.Без названия"></script>
                	<script src="./Купить чехлы для Huawei_Honor_files/jquery.inputmask.js.Без названия" type="text/javascript"></script>
                	<script src="./Купить чехлы для Huawei_Honor_files/jquery.inputmask.extensions.js.Без названия" type="text/javascript"></script>
                	<script src="./Купить чехлы для Huawei_Honor_files/jquery.inputmask.numeric.extensions.js.Без названия" type="text/javascript"></script>
                	<link rel="stylesheet" type="text/css" href="./Купить чехлы для Huawei_Honor_files/jquery.fancybox-1.3.4.css" media="screen">
                <script type="text/javascript" src="./Купить чехлы для Huawei_Honor_files/jquery.mousewheel-3.0.4.pack.js.Без названия"></script>
                	<script type="text/javascript" src="./Купить чехлы для Huawei_Honor_files/jquery.fancybox-1.3.4.js.Без названия"></script>
                	<script type="text/javascript" src="./Купить чехлы для Huawei_Honor_files/playback.js.Без названия"></script>
                	<script>
                  $( function() {
                    $( "#accordion" ).accordion({
                      heightStyle: "content",
                	  collapsible: true,
                	  active : false,
                	  activate: function( event, ui ) {
                         if ($(ui.newHeader).offset() != null) {
                        ui.newHeader,
                        $("html, body").animate({scrollTop: ($(ui.newHeader).offset().top)}, 500);
                      }
                    }
                    });
                	} );
                	$( function() {
                    var icons = {
                      header: "ui-icon-circle-arrow-e",
                      activeHeader: "ui-icon-circle-arrow-s"
                    };
                    $( "#accordion" ).accordion({
                      icons: icons
                    });
                    $( "#toggle" ).button().on( "click", function() {
                      if ( $( "#accordion" ).accordion( "option", "icons" ) ) {
                        $( "#accordion" ).accordion( "option", "icons", null );
                      } else {
                        $( "#accordion" ).accordion( "option", "icons", icons );
                      }
                    });
                  } );
                  </script>
                  <script type="text/javascript">
                  $(function() {
                \s
                $(window).scroll(function() {
                \s
                if($(this).scrollTop() != 0) {
                \s
                $('#toTop').fadeIn();
                \s
                } else {
                \s
                $('#toTop').fadeOut();
                \s
                }
                \s
                });
                \s
                $('#toTop').click(function() {
                \s
                $('body,html').animate({scrollTop:0},800);
                \s
                });
                \s
                });
                \s
                </script>
                <link rel="stylesheet" href="./Купить чехлы для Huawei_Honor_files/widget.css" class="jv-css"><link rel="stylesheet" type="text/css" href="./Купить чехлы для Huawei_Honor_files/omnichannelMenu.widget.css"></head>
                <body class="body_undertop" topmargin="0" leftmargin="0" bottommargin="0" rightmargin="0" align="center">
                                
                <table class="table1" style="box-shadow:0px 0px 32px #595959; margin:5px auto; " bgcolor="#ffffff" width="1024" border="0" cellpadding="0" cellspacing="0" align="center">
                  <tbody><tr>
                   <td colspan="3" width="1024">
                  <table width="100%" border="0" height="110px" cellpadding="0" cellspacing="0" style="margin-top: 0px; margin-bottom: 0px;">
                  <tbody><tr>
                    <td width="365px" rowspan="2" align="left">
                		<table width="250px" align="left"><tbody><tr><td width="60px" height="60px"><img onclick="document.location=&#39;http://www.playback.ru&#39;;return false" src="./Купить чехлы для Huawei_Honor_files/lolo.png" class="logotip" alt="Playback.ru - фотоаппараты, видеокамеры и аксессуары к ним" title="Playback.ru - фотоаппараты, видеокамеры и аксессуары к ним"> </td><td valign="center" align="left"><a class="tele_span" href="https://www.playback.ru/"><span class="tele_span_playback">PlayBack.ru</span></a><br><span style="cursor: pointer;" onclick="document.location=&#39;/waytoplayback.html&#39;;return false" class="getcallback2">5 минут от метро ВДНХ</span></td></tr>
                		</tbody></table>
                	</td>
                	<td width="3px" rowspan="2" align="center">&nbsp;
                    </td>
                    <td width="290px" rowspan="2">
                		<table width="215px" align="center"><tbody><tr><td valign="center" align="center"><span class="tele_span"><nobr><a href="tel:+74951437771">8(495)143-77-71</a></nobr></span><span class="grrafik"><nobr><br>пн-пт: c 11 до 20<br>сб-вс: с 11 до 18</nobr></span></td></tr>
                		</tbody></table>
                    </td>
                    <td width="3px" align="center" rowspan="2">&nbsp;
                    </td>
                    <td width="185px">
                		<table width="175px" align="center"><tbody><tr><td valign="center" align="center"><span class="blocknamezpom" style="cursor: pointer;" onclick="document.location=&#39;/tell_about_the_problem.html&#39;;return false">Возникла проблема?<br>Напишите нам!</span></td></tr>
                		</tbody></table>
                    <span class="tele_span"></span>
                  \s
                    </td>
                    <td width="3px" align="center">&nbsp;
                    </td>
                	<td width="179px">
                	<table width="175px" align="center"><tbody><tr><td width="53px" height="50px" rowspan="2" align="left"><a href="https://www.playback.ru/basket.html"><img src="./Купить чехлы для Huawei_Honor_files/basket.png" width="49px" border="0"></a></td><td valign="bottom" align="left" height="25px"><a class="tele_span2" href="https://www.playback.ru/basket.html">Корзина</a><br><span class="take_me_call"></span></td></tr>
                	<tr>
                	            <td height="10px" align="right" valign="top"><span class="basket_inc_label" id="sosotoyaniekorziny">пуста</span></td>
                	</tr></tbody></table>
                	</td>
                	</tr>
                	<tr>
                    <td colspan="3" style="text-align: right;">
                	<form action="https://www.playback.ru/search.php" method="get" class="izkat">
                  <input type="search" name="search_string" placeholder="поиск" class="ssstring">
                  <input type="submit" name="" value="Искать" class="iskat">
                </form></td>
                   </tr>
                	</tbody></table>
                	</td>
                	<!---	<tr>\s
                	<td colspan="3" style="color: #2556A3; font:17px Roboto-Regular,Helvetica,sans-serif; text-align: center; height: 35px;vertical-align: middle;padding-bottom:10px;">
                		<b>Уважаемые покупатели! Наш график работы в праздничные дни:<br /> 31 декабря - с 11:00 до 15:00, 1 января - выходной, со 2 по 7 января - с 11:00 до 18:00</b>
                	</td>
                  </tr>--->
                  </tr><tr>
                    <td colspan="3" style="text-align: center;">
                	
                	
                	
                	
                	
                	<nav>
                  <ul class="topmenu">
                    <li><a href="https://www.playback.ru/catalog/1310.html/" class="active" onclick="return false;"><img src="./Купить чехлы для Huawei_Honor_files/imglist.png" height="9px"> Каталог<span class="fa fa-angle-down"></span></a>
                      <ul class="submenu">
                <li><a href="https://www.playback.ru/catalog/1652.html">Чехлы для смартфонов Infinix</a></li><li><a href="https://www.playback.ru/catalog/1511.html">Смартфоны</a></li><li><a href="https://www.playback.ru/catalog/1300.html">Чехлы для смартфонов Xiaomi</a></li><li><a href="https://www.playback.ru/catalog/1302.html">Защитные стекла для смартфонов Xiaomi</a></li><li><a href="https://www.playback.ru/catalog/1310.html">Чехлы для Huawei/Honor</a></li><li><a href="https://www.playback.ru/catalog/1308.html">Чехлы для смартфонов Samsung</a></li><li><a href="https://www.playback.ru/catalog/1307.html">Защитные стекла для смартфонов Samsung</a></li><li><a href="https://www.playback.ru/catalog/1141.html">Планшеты</a></li><li><a href="https://www.playback.ru/catalog/1315.html">Зарядные устройства и кабели</a></li><li><a href="https://www.playback.ru/catalog/1329.html">Держатели для смартфонов</a></li><li><a href="https://www.playback.ru/catalog/665.html">Автодержатели</a></li><li><a href="https://www.playback.ru/catalog/1304.html">Носимая электроника</a></li><li><a href="https://www.playback.ru/catalog/1305.html">Наушники и колонки</a></li><li><a href="https://www.playback.ru/catalog/805.html">Запчасти для телефонов</a></li><li><a href="https://www.playback.ru/catalog/1311.html">Чехлы для планшетов</a></li><li><a href="https://www.playback.ru/catalog/1317.html">Аксессуары для фото-видео</a></li><li><a href="https://www.playback.ru/catalog/1318.html">Чехлы для смартфонов Apple</a></li><li><a href="https://www.playback.ru/catalog/1429.html">USB Флеш-накопители</a></li><li><a href="https://www.playback.ru/catalog/1473.html">Товары для детей</a></li><li><a href="https://www.playback.ru/catalog/1507.html">Защитные стекла для смартфонов Realme</a></li><li><a href="https://www.playback.ru/catalog/1508.html">Чехлы для смартфонов Realme</a></li><li><a href="https://www.playback.ru/catalog/18.html">Карты памяти</a></li><li><a href="https://www.playback.ru/catalog/1303.html">Защитные стекла для планшетов</a></li><li><a href="https://www.playback.ru/catalog/1312.html">Защитные стекла для смартфонов</a></li><li><a href="https://www.playback.ru/catalog/1622.html">Защитные стекла для смартфонов Apple</a></li><li><a href="https://www.playback.ru/catalog/1626.html">Чехлы для смартфонов Vivo</a></li><li><a href="https://www.playback.ru/catalog/1636.html">Чехлы для смартфонов Tecno</a></li>      </ul>
                    </li>
                    <li><a href="https://www.playback.ru/dostavka.html">Доставка</a></li>
                    <li><a href="https://www.playback.ru/pickup.html">Самовывоз</a></li>
                    <li><a href="https://www.playback.ru/payment.html">Оплата</a></li>
                    <li><a href="https://www.playback.ru/warranty.html">Гарантия и обмен</a></li>
                    <li><a href="https://www.playback.ru/contacts.html">Контакты</a></li>
                  </ul>
                </nav>
                	
                	
                	
                	
                	
                	</td>
                  </tr>
                    <tr><td colspan="3" valign="top">
                	<table width="100%" border="0" cellpadding="0" cellspacing="0">
                	<tbody><tr><!----<td class="menu_full_cell" width="253">---->		
                		
                    <td colspan="2" class="item_full_cell" itemscope="" itemtype="http://schema.org/ItemList">
                	<link itemprop="url" href="http://www.playback.ru/catalog/1310.html/">
                    <table width="100%" border="0" cellpadding="0" cellspacing="0">
                    <tbody><tr>
                        <td colspan="2" class="full_route2">
                		<div style="width: 100%; text-align: left; padding-top: 5px;">
                				<a class="button15" href="https://www.playback.ru/">◄ Наши спецпредложения</a>		
                				</div>
                        </td>
                		</tr>
                		<!---<tr>
                			 <td colspan="2" height="210px">
                			 <div id="featured">\s
                				<img src="/promotion/dslr_plus_card.jpg" alt="Скидка на  карту при покупке зеркального фотоаппарата" />
                				<img src="/promotion/foto_plus_card.jpg" alt="Скидка на чехол карту при покупке компактного фотоаппарата" />
                				<img src="/promotion/video_plus_card.jpg" alt="Скидка на  карту при покупке видеокамеры" />
                			</div>
                			  </td>
                			</tr>--->
                	<tr>
                        <td colspan="2" class="catalog_name">
                		<h1>Чехлы для Huawei/Honor</h1>	
                	    </td>
                		</tr>
                		<tr>
                        <td colspan="2" class="reg_content_otb">
                <table width="100%" border="0" cellspacing="0" cellpadding="0"><tbody><tr><td width="210px"><img src="./Купить чехлы для Huawei_Honor_files/1310.jpg"></td><td class="opissub">&nbsp;</td></tr><tr><td colspan="2"><hr></td></tr><tr><td colspan="2"><table width="1030px" border="0" cellspacing="0" cellpadding="0"><tbody><tr><td class="catalog_content_cell" width="195px"><img onclick="document.location=&#39;/catalog/1113.html&#39;;return false" width="175px" src="./Купить чехлы для Huawei_Honor_files/1113.jpg" style="border:none; decoration: none; cursor: pointer;"><br><br><a href="https://www.playback.ru/catalog/1113.html">Чехлы для Honor 10 Lite</a><br></td><td class="catalog_content_cell" width="195px"><img onclick="document.location=&#39;/catalog/1500.html&#39;;return false" width="175px" src="./Купить чехлы для Huawei_Honor_files/1500.jpg" style="border:none; decoration: none; cursor: pointer;"><br><br><a href="https://www.playback.ru/catalog/1500.html">Чехлы для Honor 50</a><br></td><td class="catalog_content_cell" width="195px"><img onclick="document.location=&#39;/catalog/1530.html&#39;;return false" width="175px" src="./Купить чехлы для Huawei_Honor_files/1530.jpg" style="border:none; decoration: none; cursor: pointer;"><br><br><a href="https://www.playback.ru/catalog/1530.html">Чехлы для Honor 50 Lite</a><br></td><td class="catalog_content_cell" width="195px"><img onclick="document.location=&#39;/catalog/1125.html&#39;;return false" width="175px" src="./Купить чехлы для Huawei_Honor_files/1125.jpg" style="border:none; decoration: none; cursor: pointer;"><br><br><a href="https://www.playback.ru/catalog/1125.html">Чехлы для Honor 7A/Huawei Y5 Prime (2018)</a><br></td></tr><tr><td class="catalog_content_cell" width="195px"><img onclick="document.location=&#39;/catalog/1120.html&#39;;return false" width="175px" src="./Купить чехлы для Huawei_Honor_files/1120.jpg" style="border:none; decoration: none; cursor: pointer;"><br><br><a href="https://www.playback.ru/catalog/1120.html">Чехлы для Honor 8C</a><br></td><td class="catalog_content_cell" width="195px"><img onclick="document.location=&#39;/catalog/1324.html&#39;;return false" width="175px" src="./Купить чехлы для Huawei_Honor_files/1324.jpg" style="border:none; decoration: none; cursor: pointer;"><br><br><a href="https://www.playback.ru/catalog/1324.html">Чехлы для Honor 8S</a><br></td><td class="catalog_content_cell" width="195px"><img onclick="document.location=&#39;/catalog/1116.html&#39;;return false" width="175px" src="./Купить чехлы для Huawei_Honor_files/1116.jpg" style="border:none; decoration: none; cursor: pointer;"><br><br><a href="https://www.playback.ru/catalog/1116.html">Чехлы для Honor 9 Lite</a><br></td><td class="catalog_content_cell" width="195px"><img onclick="document.location=&#39;/catalog/1117.html&#39;;return false" width="175px" src="./Купить чехлы для Huawei_Honor_files/1117.jpg" style="border:none; decoration: none; cursor: pointer;"><br><br><a href="https://www.playback.ru/catalog/1117.html">Чехлы для Huawei Mate 20 Lite</a><br></td></tr><tr><td class="catalog_content_cell" width="195px"><img onclick="document.location=&#39;/catalog/1118.html&#39;;return false" width="175px" src="./Купить чехлы для Huawei_Honor_files/1118.jpg" style="border:none; decoration: none; cursor: pointer;"><br><br><a href="https://www.playback.ru/catalog/1118.html">Чехлы для Huawei P Smart (2019)</a><br></td><td class="catalog_content_cell" width="195px"><img onclick="document.location=&#39;/catalog/1119.html&#39;;return false" width="175px" src="./Купить чехлы для Huawei_Honor_files/1119.jpg" style="border:none; decoration: none; cursor: pointer;"><br><br><a href="https://www.playback.ru/catalog/1119.html">Чехлы для Huawei P20 Lite</a><br></td><td class="catalog_content_cell" width="195px"><img onclick="document.location=&#39;/catalog/1325.html&#39;;return false" width="175px" src="./Купить чехлы для Huawei_Honor_files/1325.jpg" style="border:none; decoration: none; cursor: pointer;"><br><br><a href="https://www.playback.ru/catalog/1325.html">Чехлы для Huawei Y5 (2019)</a><br></td><td class="catalog_content_cell" width="195px"><img onclick="document.location=&#39;/catalog/1135.html&#39;;return false" width="175px" src="./Купить чехлы для Huawei_Honor_files/1135.jpg" style="border:none; decoration: none; cursor: pointer;"><br><br><a href="https://www.playback.ru/catalog/1135.html">Чехлы для Huawei Y6 (2019)</a><br></td></tr><tr><td class="catalog_content_cell" width="195px"><img onclick="document.location=&#39;/catalog/1149.html&#39;;return false" width="175px" src="./Купить чехлы для Huawei_Honor_files/1149.jpg" style="border:none; decoration: none; cursor: pointer;"><br><br><a href="https://www.playback.ru/catalog/1149.html">Чехлы для Huawei Y7 (2019)</a><br></td><td class="clear_spec_cell" width="195px">&nbsp;</td><td class="clear_spec_cell" width="195px">&nbsp;</td><td class="clear_spec_cell" width="195px">&nbsp;</td></tr>
                			</tbody></table></td></tr></tbody></table>  </td>
                      </tr></tbody></table></td></tr><tr>    <td colspan="3" align="center">
                <div class="footer">
                <div class="footer_block">
                <span class="footer_h1">Информация</span>
                <br>
                <a href="https://www.playback.ru/">Наши спецпредложения</a>
                <br>
                <a href="https://www.playback.ru/dostavka.html">Доставка</a>
                <br>
                <a href="https://www.playback.ru/payment.html">Оплата</a>
                <br>
                <a href="https://www.playback.ru/warranty.html">Гарантия</a>
                <br>
                <a href="https://www.playback.ru/contacts.html">Контакты</a>
                <br>
                <a href="https://www.playback.ru/privacy_policy.html">Положение о конфиденциальности и защите персональных данных</a>
                </div>
                <div class="footer_block_cont">
                <span class="footer_tel">+7(495)143-77-71</span>
                <br><br>
                <a class="footer_email" href="http://vk.com/playback_ru" target="_blank"><img src="./Купить чехлы для Huawei_Honor_files/VK.png" title="Наша страница Вконтакте"></a>
                &nbsp;&nbsp;
                <br><br>
                                
                </div>
                <div class="footer_block_cont" style="width:260px;">
                <span class="footer_h1">График работы:</span>
                <br>
                пн-пт: c 11-00 до 20-00
                <br>
                сб-вс: с 11-00 до 18-00
                <br><br>
                <span class="footer_h1">Наш адрес:</span>
                <br>
                Москва, Звездный бульвар, 10,
                <br>
                строение 1, 2 этаж, офис 10.
                </div>
                <div class="footer_block">
                                
                </div>
                                
                <div class="footer_block">
                <script type="text/javascript" src="./Купить чехлы для Huawei_Honor_files/openapi.js.Без названия"></script>
                <div id="vk_groups" style="width: 260px; height: 215.988px; background: none;"><iframe name="fXD6bcbc" frameborder="0" src="./Купить чехлы для Huawei_Honor_files/widget_community.html" width="260" height="210" scrolling="no" id="vkwidget1" style="overflow: hidden; height: 215.988px;"></iframe></div>
                <script type="text/javascript">
                VK.Widgets.Group("vk_groups", {mode: 0, width: "260", height: "210", color1: 'FFFFFF', color2: '0C5696', color3: '0064BA'}, 48023501);
                </script>
                </div>
                </div>
                <div style="width: 1024px; font-family: Roboto-Regular,Helvetica,sans-serif; text-align: right; font-size: 12px; text-align: left; padding-left: 10px; color: #595959; background: url(/img/footer-fon.png) repeat;">
                2005-2024 ©Интернет магазин PlayBack.ru
                </div>
                <!-- Yandex.Metrika counter -->
                <script type="text/javascript">
                   (function(m,e,t,r,i,k,a){m[i]=m[i]||function(){(m[i].a=m[i].a||[]).push(arguments)};
                   m[i].l=1*new Date();k=e.createElement(t),a=e.getElementsByTagName(t)[0],k.async=1,k.src=r,a.parentNode.insertBefore(k,a)})
                   (window, document, "script", "https://mc.yandex.ru/metrika/tag.js", "ym");
                                
                   ym(232370, "init", {
                        clickmap:true,
                        trackLinks:true,
                        accurateTrackBounce:true,
                        webvisor:true
                   });
                </script>
                <noscript><div><img src="https://mc.yandex.ru/watch/232370" style="position:absolute; left:-9999px;" alt="" /></div></noscript>
                <!-- /Yandex.Metrika counter -->
                <!-- BEGIN JIVOSITE CODE {literal} -->
                <script type="text/javascript">
                (function(){ var widget_id = '8LKJc6dMce';var d=document;var w=window;function l(){
                  var s = document.createElement('script'); s.type = 'text/javascript'; s.async = true;
                  s.src = '//code.jivosite.com/script/widget/'+widget_id
                    ; var ss = document.getElementsByTagName('script')[0]; ss.parentNode.insertBefore(s, ss);}
                  if(d.readyState=='complete'){l();}else{if(w.attachEvent){w.attachEvent('onload',l);}
                  else{w.addEventListener('load',l,false);}}})();
                </script>
                <!-- {/literal} END JIVOSITE CODE -->
                </td>
                  </tr>
                </tbody></table>
                <a href="https://www.playback.ru/catalog/1310.html/#" class="scrollup">Наверх</a>
                                
                </td></tr></tbody></table><div id="fancybox-tmp"></div><div id="fancybox-loading"><div></div></div><div id="fancybox-overlay"></div><div id="fancybox-wrap"><div id="fancybox-outer"><div class="fancybox-bg" id="fancybox-bg-n"></div><div class="fancybox-bg" id="fancybox-bg-ne"></div><div class="fancybox-bg" id="fancybox-bg-e"></div><div class="fancybox-bg" id="fancybox-bg-se"></div><div class="fancybox-bg" id="fancybox-bg-s"></div><div class="fancybox-bg" id="fancybox-bg-sw"></div><div class="fancybox-bg" id="fancybox-bg-w"></div><div class="fancybox-bg" id="fancybox-bg-nw"></div><div id="fancybox-content"></div><a id="fancybox-close"></a><div id="fancybox-title"></div><a href="javascript:;" id="fancybox-left"><span class="fancy-ico" id="fancybox-left-ico"></span></a><a href="javascript:;" id="fancybox-right"><span class="fancy-ico" id="fancybox-right-ico"></span></a></div></div><div id="jivo-iframe-container" style="opacity: 0; visibility: hidden; width: 0px; height: 0px; overflow: hidden;"><iframe src="./Купить чехлы для Huawei_Honor_files/saved_resource.html" role="presentation" allow="autoplay" title="Jivochat" name="jivo_container" id="jivo_container" frameborder="no"></iframe></div><jdiv><jdiv class="globalClass_ebf4"><jdiv translate="no" class="notranslate" style="animation-duration: 300ms; animation-timing-function: cubic-bezier(0.39, 0.24, 0.21, 0.99); animation-delay: 0s; animation-iteration-count: 1; animation-direction: normal; animation-fill-mode: both; animation-play-state: running; animation-name: Label_CLOSE_WIDGET_a1d7; display: block; z-index: 2147483646; position: fixed;"><jdiv class="label_a2ed _right_e9c0 __show_c3bb notranslate" translate="no" id="jvlabelWrap" style="max-width: calc(-40px + 100vh); right: 0px; transform-origin: right bottom; bottom: 20px; transform: rotate(-90deg) translateX(100%); background: linear-gradient(95deg, rgb(37, 86, 163) 20%, rgb(37, 86, 163) 80%);"><jdiv class="hoverl_d621"><jdiv class="omnichannel_e66c right_d8be"><jdiv class="root_f434 __lgRadius_a528" style="box-shadow: rgba(0, 0, 0, 0.1) 0px 1px 17px 1px, rgba(0, 0, 0, 0.16) 0px 0px 4px; width: 280px; transform-origin: right top; transform: rotate(90deg) translate(-16px, calc(-100% + 280px));"><jdiv class="menuWrapper_ac81 desktop_f453"><jdiv class="menu_c0b4"><a href="https://t.me/playbackimbot" target="_blank" rel="nofollow noopener noreferrer" class="link_fe17 item_ca49"><jdiv class="desktop_ab2d"><jdiv class="icon_fd7f" style="background-image: url(&quot;data:image/svg+xml,%3Csvg%20width%3D%2224%22%20height%3D%2224%22%20fill%3D%22none%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Cpath%20d%3D%22M0%2012C0%205.373%205.373%200%2012%200s12%205.373%2012%2012-5.373%2012-12%2012S0%2018.627%200%2012Z%22%20fill%3D%22%23fff%22%2F%3E%3Cpath%20fill-rule%3D%22evenodd%22%20clip-rule%3D%22evenodd%22%20d%3D%22M24%2012c0%206.627-5.373%2012-12%2012S0%2018.627%200%2012%205.373%200%2012%200s12%205.373%2012%2012ZM12.43%208.859c-1.167.485-3.5%201.49-6.998%203.014-.568.226-.866.447-.893.663-.046.366.412.51%201.034.705.085.027.173.054.263.084.613.199%201.437.432%201.865.441.389.008.823-.152%201.302-.48%203.268-2.207%204.955-3.322%205.061-3.346.075-.017.179-.039.249.024.07.062.063.18.056.212-.046.193-1.84%201.862-2.77%202.726-.29.269-.495.46-.537.504-.094.097-.19.19-.282.279-.57.548-.996.96.024%201.632.49.323.882.59%201.273.856.427.291.853.581%201.405.943.14.092.274.187.405.28.497.355.944.673%201.496.623.32-.03.652-.331.82-1.23.397-2.126%201.179-6.73%201.36-8.628a2.112%202.112%200%200%200-.02-.472.506.506%200%200%200-.172-.325c-.143-.117-.365-.142-.465-.14-.451.008-1.143.249-4.476%201.635Z%22%20fill%3D%22%2354A6E4%22%2F%3E%3C%2Fsvg%3E&quot;);"></jdiv><jdiv class="title_b935">Telegram</jdiv></jdiv></a><a href="https://vk.com/im?sel=-48023501" target="_blank" rel="nofollow noopener noreferrer" class="link_fe17 item_ca49"><jdiv class="desktop_ab2d"><jdiv class="icon_fd7f" style="background-image: url(&quot;data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20fill%3D%22none%22%20viewBox%3D%220%200%2016%2016%22%3E%3Cmask%20id%3D%22a%22%20width%3D%2214%22%20height%3D%2214%22%20x%3D%221%22%20y%3D%221%22%20maskUnits%3D%22userSpaceOnUse%22%20style%3D%22mask-type%3Aluminance%22%3E%3Cpath%20fill%3D%22%23fff%22%20d%3D%22M14.9307%201H1.06934v14H14.9307V1Z%22%2F%3E%3C%2Fmask%3E%3Cg%20mask%3D%22url(%23a)%22%3E%3Cpath%20fill%3D%22%2307F%22%20d%3D%22M1.06934%207.72c0-3.16784%200-4.75175.97437-5.73588C3.01809%201%204.58633%201%207.7228%201h.55446c3.13644%200%204.70474%200%205.67904.98412.9744.98413.9744%202.56804.9744%205.73588v.56c0%203.1678%200%204.7518-.9744%205.7359C12.982%2015%2011.4137%2015%208.27726%2015H7.7228c-3.13647%200-4.70471%200-5.67909-.9841-.97437-.9841-.97437-2.5681-.97437-5.7359v-.56Z%22%2F%3E%3Cpath%20fill%3D%22%23fff%22%20d%3D%22M8.44475%2011.0859c-3.15925%200-4.96122-2.18754-5.0363-5.82754h1.58251c.05198%202.67166%201.21862%203.80333%202.14271%204.03666V5.25836h1.49015v2.30415c.91254-.09916%201.87118-1.14915%202.19458-2.30415h1.4902c-.2484%201.42334-1.288%202.47333-2.0273%202.90499.7393.35%201.9234%201.26584%202.3739%202.92255h-1.6404c-.3523-1.10838-1.23009-1.96588-2.39098-2.08255v2.08255h-.17907Z%22%2F%3E%3C%2Fg%3E%3C%2Fsvg%3E&quot;);"></jdiv><jdiv class="title_b935">Сообщение ВКонтакте<jdiv class="subTitle_f070">Обычно отвечаем в течение одного дня</jdiv></jdiv></jdiv></a><jdiv class="item_ca49"><jdiv class="desktop_ab2d"><jdiv class="icon_fd7f" style="background-image: url(&quot;data:image/svg+xml,%3Csvg%20width%3D%2224%22%20height%3D%2224%22%20fill%3D%22none%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Cpath%20fill-rule%3D%22evenodd%22%20clip-rule%3D%22evenodd%22%20d%3D%22M12%2017.86c4.97%200%209-3.297%209-7.364s-4.03-7.364-9-7.364-9%203.297-9%207.364c0%202.452%201.465%204.624%203.717%205.962-.677%202.418-1.837%204.41-1.837%204.41s3.568-.864%206.327-3.037c.26.019.526.029.793.029Z%22%20fill%3D%22%2318c139%22%2F%3E%3C%2Fsvg%3E&quot;);"></jdiv><jdiv class="title_b935">Написать в чат</jdiv></jdiv></jdiv></jdiv></jdiv></jdiv></jdiv></jdiv><jdiv class="text_f41a contentTransitionWrap_a6fb" style="font-size: 12px; font-family: Helvetica, Arial; font-style: normal; color: rgb(240, 241, 241);">Есть вопросы? Задавайте!</jdiv><jdiv class="copy_a0bf"><jdiv class="logo_d386 _right_ff31"><jdiv class="hoverBox_be0f"></jdiv><jdiv class="logoIcon_a528" style="background-image: url(&quot;data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22108%22%20height%3D%2236%22%20viewBox%3D%220%200%20108%2036%22%3E%0A%20%20%20%20%3Cg%3E%0A%20%20%20%20%20%20%20%20%3Cpath%20fill%3D%22%23F0F1F1%22%20fill-rule%3D%22evenodd%22%20d%3D%22M6.342%2011.967H.957c-.2%200-.4%200-.6.2%200%20.2-.198.2-.198.4v1.993c0%20.2%200%20.4.198.6.2.198.4.198.6.198h2.79v14.36c0%201.397-1.196%202.594-2.392%202.594h-.4c-.2%200-.398%200-.598.2-.2.2-.2.398-.2.598v1.994c0%20.2%200%20.4.2.6.2.198.4.198.6.198h.198c3.192%200%205.785-2.792%205.785-5.984V12.566c0-.2%200-.4-.2-.6h-.398m6.98%200H11.33c-.2%200-.2%200-.4.2s-.2.2-.2.4v14.76c0%20.2%200%20.398.2.598.2.2.4.2.598.2h1.795c.2%200%20.4%200%20.598-.2.2-.2.2-.4.2-.598v-14.76c0-.2%200-.4-.198-.6h-.6m17.154%200H28.88c-.598%200-.997.2-.997.4l-4.388%209.973-4.188-9.974c0-.4-.4-.4-.6-.4h-1.993c-.4%200-.598%200-.598.2-.2.2-.2.4%200%20.6l6.78%2015.357c.2.2.4.4.6.4h.2c.198%200%20.597-.2.597-.4l6.782-15.358c.2-.2.2-.4%200-.598-.2-.2-.4-.2-.6-.2m9.176-.4c-2.193.002-3.988.8-5.584%202.594-1.595%201.597-2.393%203.592-2.393%205.985%200%202.393.798%204.388%202.393%205.984%201.596%201.794%203.39%202.592%205.585%202.592s3.99-.798%205.586-2.593c1.596-1.796%202.394-3.79%202.394-5.985%200-2.393-.798-4.388-2.394-5.984-1.596-1.794-3.59-2.591-5.585-2.591m3.193%2012.165c-.798.998-1.995%201.596-3.192%201.596-1.395%200-2.392-.398-3.39-1.395-.797-.998-1.396-2.194-1.396-3.79%200-1.396.4-2.792%201.397-3.59.798-.997%201.995-1.396%203.39-1.396%201.398%200%202.395.398%203.193%201.395.797.997%201.396%202.194%201.396%203.59s-.6%202.593-1.397%203.59m19.147-.598c-.398-.4-.797-.2-.997%200l-.4.4-.597.597c-.2.2-.4.2-.6.4-.198.2-.597.398-.796.398-.4%200-.6.2-.998.2-1.396%200-2.393-.6-3.19-1.596-.8-.998-1.397-2.194-1.397-3.79%200-1.396.4-2.792%201.396-3.79.998-.997%201.996-1.396%203.392-1.396%201.197%200%202.194.6%203.19%201.596.4.4.8.4.998.2l1.197-1.197s0-.4-.4-.8c-1.395-1.794-3.39-2.79-5.584-2.79-2.194-.002-3.99.796-5.584%202.59-1.597%201.597-2.395%203.592-2.395%205.985%200%202.393.798%204.388%202.394%206.183%201.594%201.596%203.39%202.394%205.583%202.394%202.593%200%204.588-1.197%205.984-3.39.2-.4.2-.8-.2-1.198l-.997-.997m11.57-11.569c-1.597%200-2.993.6-4.59%201.796V4.986c0-.2%200-.4-.198-.598-.2-.2-.4-.2-.6-.2H66.38c-.2%200-.398%200-.598.2-.2.2-.2.4-.2.598v22.54c0%20.198%200%20.398.2.597.2.2.4.2.598.2h1.796c.598%200%20.797-.2.797-.798V17.353c.2-.6.8-.998%201.398-1.596.797-.598%201.795-.798%202.792-.798.997%200%201.795.398%202.393%201.196.6.798.798%201.795.798%203.19v8.378c0%20.2%200%20.4.2.6.2.198.4.198.6.198h1.794c.2%200%20.4%200%20.6-.2.198-.2.198-.398.198-.598v-8.177c-.2-5.386-2.194-7.979-6.183-7.979m20.144%201.995c-.4-.798-.997-1.197-1.596-1.596-.798-.4-1.795-.598-2.992-.598-1.994%200-3.79.398-5.385.996-.398.2-.597.4-.597.998l.4%201.396c.198.598.398.798.796.598a12.823%2012.823%200%200%201%204.388-.798c.997%200%201.595.2%201.994.798.4.598.598%201.596.4%202.793l-.4-.2c-.2%200-.598-.198-1.197-.198-.4.2-.797.2-1.396.2-1.995%200-3.392.398-4.588%201.395-1.197.997-1.596%202.194-1.596%203.79s.4%202.992%201.396%203.99c.998.996%202.194%201.395%203.59%201.395%201.796%200%203.39-.598%204.787-1.994l.398%201.196c.2.4.4.6.6.6h1.195c.2%200%20.4%200%20.6-.2.198-.2.198-.4.198-.6V18.75c0-1.198%200-2.195-.198-2.993-.2-.598-.4-1.396-.798-2.194M91.31%2023.735c-.2.4-.797.998-1.396%201.396-.798.4-1.396.6-2.194.6s-1.396-.2-1.795-.798c-.4-.4-.598-1.197-.598-1.795%200-.798.2-1.396.798-1.995.598-.4%201.396-.797%202.393-.797.598%200%201.197%200%201.795.2.6.198.997.198.997.398v2.792m16.555%202.793l-.598-1.596c-.2-.4-.4-.598-.997-.4a3.834%203.834%200%200%201-2.394.8c-.4%200-.798-.2-.997-.4-.2-.2-.4-.598-.4-1.396V14.96h4.19c.198%200%20.397%200%20.597-.2.2-.2.2-.4.2-.6v-1.594c0-.2%200-.4-.2-.6h-4.788V7.38c0-.2%200-.4-.2-.598-.2-.2-.398-.2-.598-.2h-1.994c-.2%200-.4%200-.6.2-.198.2-.198.398-.198.598v4.587h-1.795c-.4%200-.798.2-.798.798v1.396c0%20.2%200%20.4.2.6.2.2.398.2.598.2h1.795v8.975c0%201.595.2%202.792.798%203.59.598.798%201.595%201.197%202.99%201.197.8%200%201.597-.2%202.395-.4s1.396-.598%201.795-.797c.997-.2%201.197-.598.997-.997z%22%2F%3E%0A%20%20%20%20%20%20%20%20%3Cpath%20fill%3D%22%2318c139%22%20fill-rule%3D%22evenodd%22%20d%3D%22M6.94%2010.77C6.543.4.16%200%20.16%200c-.4%209.374%206.78%2010.77%206.78%2010.77z%22%2F%3E%0A%20%20%20%20%3C%2Fg%3E%0A%3C%2Fsvg%3E%0A&quot;);"></jdiv><jdiv class="fallback_b179"></jdiv></jdiv></jdiv><jdiv class="leafCont_afcc"><jdiv class="leaf_e633 _right_c247"><jdiv class="cssLeaf_a0fe" style="background-image: url(&quot;data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%2232%22%20height%3D%2240%22%20viewBox%3D%220%200%2032%2040%22%3E%0A%20%20%20%20%3Cg%20fill%3D%22none%22%20fill-rule%3D%22evenodd%22%3E%0A%20%20%20%20%20%20%20%20%3Cpath%20fill%3D%22%232556A3%22%20d%3D%22M0%200h9.02L32%2033.196V40H0z%22%2F%3E%0A%20%20%20%20%20%20%20%20%3Cpath%20fill%3D%22%2318c139%22%20d%3D%22M9%200c3.581.05%2023%205.426%2023%2033.08v.03C18.922%2030.751%209%2019.311%209%205.554V0z%22%2F%3E%0A%20%20%20%20%3C%2Fg%3E%0A%3C%2Fsvg%3E%0A&quot;);"></jdiv></jdiv></jdiv><jdiv class="copyContainer_d2f0"><jdiv class="copyright_ba6c _right_cbd6" style="background: linear-gradient(95deg, rgb(37, 86, 163) 20%, rgb(37, 86, 163) 80%);"><jdiv class="text_a56e" style="color: rgb(240, 241, 241);"><a href="https://www.jivo.ru/i_sa/?utm_source=www.playback.ru&amp;utm_medium=link&amp;utm_content=label_tooltip&amp;utm_campaign=from_widget" rel="nofollow noopener noreferrer" target="_blank" class="link_cb83">Бизнес-мессенджер <jdiv class="logoIcon_ffe3" style="background-image: url(&quot;data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22108%22%20height%3D%2236%22%20viewBox%3D%220%200%20108%2036%22%3E%0A%20%20%20%20%3Cg%3E%0A%20%20%20%20%20%20%20%20%3Cpath%20fill%3D%22%23F0F1F1%22%20fill-rule%3D%22evenodd%22%20d%3D%22M6.342%2011.967H.957c-.2%200-.4%200-.6.2%200%20.2-.198.2-.198.4v1.993c0%20.2%200%20.4.198.6.2.198.4.198.6.198h2.79v14.36c0%201.397-1.196%202.594-2.392%202.594h-.4c-.2%200-.398%200-.598.2-.2.2-.2.398-.2.598v1.994c0%20.2%200%20.4.2.6.2.198.4.198.6.198h.198c3.192%200%205.785-2.792%205.785-5.984V12.566c0-.2%200-.4-.2-.6h-.398m6.98%200H11.33c-.2%200-.2%200-.4.2s-.2.2-.2.4v14.76c0%20.2%200%20.398.2.598.2.2.4.2.598.2h1.795c.2%200%20.4%200%20.598-.2.2-.2.2-.4.2-.598v-14.76c0-.2%200-.4-.198-.6h-.6m17.154%200H28.88c-.598%200-.997.2-.997.4l-4.388%209.973-4.188-9.974c0-.4-.4-.4-.6-.4h-1.993c-.4%200-.598%200-.598.2-.2.2-.2.4%200%20.6l6.78%2015.357c.2.2.4.4.6.4h.2c.198%200%20.597-.2.597-.4l6.782-15.358c.2-.2.2-.4%200-.598-.2-.2-.4-.2-.6-.2m9.176-.4c-2.193.002-3.988.8-5.584%202.594-1.595%201.597-2.393%203.592-2.393%205.985%200%202.393.798%204.388%202.393%205.984%201.596%201.794%203.39%202.592%205.585%202.592s3.99-.798%205.586-2.593c1.596-1.796%202.394-3.79%202.394-5.985%200-2.393-.798-4.388-2.394-5.984-1.596-1.794-3.59-2.591-5.585-2.591m3.193%2012.165c-.798.998-1.995%201.596-3.192%201.596-1.395%200-2.392-.398-3.39-1.395-.797-.998-1.396-2.194-1.396-3.79%200-1.396.4-2.792%201.397-3.59.798-.997%201.995-1.396%203.39-1.396%201.398%200%202.395.398%203.193%201.395.797.997%201.396%202.194%201.396%203.59s-.6%202.593-1.397%203.59m19.147-.598c-.398-.4-.797-.2-.997%200l-.4.4-.597.597c-.2.2-.4.2-.6.4-.198.2-.597.398-.796.398-.4%200-.6.2-.998.2-1.396%200-2.393-.6-3.19-1.596-.8-.998-1.397-2.194-1.397-3.79%200-1.396.4-2.792%201.396-3.79.998-.997%201.996-1.396%203.392-1.396%201.197%200%202.194.6%203.19%201.596.4.4.8.4.998.2l1.197-1.197s0-.4-.4-.8c-1.395-1.794-3.39-2.79-5.584-2.79-2.194-.002-3.99.796-5.584%202.59-1.597%201.597-2.395%203.592-2.395%205.985%200%202.393.798%204.388%202.394%206.183%201.594%201.596%203.39%202.394%205.583%202.394%202.593%200%204.588-1.197%205.984-3.39.2-.4.2-.8-.2-1.198l-.997-.997m11.57-11.569c-1.597%200-2.993.6-4.59%201.796V4.986c0-.2%200-.4-.198-.598-.2-.2-.4-.2-.6-.2H66.38c-.2%200-.398%200-.598.2-.2.2-.2.4-.2.598v22.54c0%20.198%200%20.398.2.597.2.2.4.2.598.2h1.796c.598%200%20.797-.2.797-.798V17.353c.2-.6.8-.998%201.398-1.596.797-.598%201.795-.798%202.792-.798.997%200%201.795.398%202.393%201.196.6.798.798%201.795.798%203.19v8.378c0%20.2%200%20.4.2.6.2.198.4.198.6.198h1.794c.2%200%20.4%200%20.6-.2.198-.2.198-.398.198-.598v-8.177c-.2-5.386-2.194-7.979-6.183-7.979m20.144%201.995c-.4-.798-.997-1.197-1.596-1.596-.798-.4-1.795-.598-2.992-.598-1.994%200-3.79.398-5.385.996-.398.2-.597.4-.597.998l.4%201.396c.198.598.398.798.796.598a12.823%2012.823%200%200%201%204.388-.798c.997%200%201.595.2%201.994.798.4.598.598%201.596.4%202.793l-.4-.2c-.2%200-.598-.198-1.197-.198-.4.2-.797.2-1.396.2-1.995%200-3.392.398-4.588%201.395-1.197.997-1.596%202.194-1.596%203.79s.4%202.992%201.396%203.99c.998.996%202.194%201.395%203.59%201.395%201.796%200%203.39-.598%204.787-1.994l.398%201.196c.2.4.4.6.6.6h1.195c.2%200%20.4%200%20.6-.2.198-.2.198-.4.198-.6V18.75c0-1.198%200-2.195-.198-2.993-.2-.598-.4-1.396-.798-2.194M91.31%2023.735c-.2.4-.797.998-1.396%201.396-.798.4-1.396.6-2.194.6s-1.396-.2-1.795-.798c-.4-.4-.598-1.197-.598-1.795%200-.798.2-1.396.798-1.995.598-.4%201.396-.797%202.393-.797.598%200%201.197%200%201.795.2.6.198.997.198.997.398v2.792m16.555%202.793l-.598-1.596c-.2-.4-.4-.598-.997-.4a3.834%203.834%200%200%201-2.394.8c-.4%200-.798-.2-.997-.4-.2-.2-.4-.598-.4-1.396V14.96h4.19c.198%200%20.397%200%20.597-.2.2-.2.2-.4.2-.6v-1.594c0-.2%200-.4-.2-.6h-4.788V7.38c0-.2%200-.4-.2-.598-.2-.2-.398-.2-.598-.2h-1.994c-.2%200-.4%200-.6.2-.198.2-.198.398-.198.598v4.587h-1.795c-.4%200-.798.2-.798.798v1.396c0%20.2%200%20.4.2.6.2.2.398.2.598.2h1.795v8.975c0%201.595.2%202.792.798%203.59.598.798%201.595%201.197%202.99%201.197.8%200%201.597-.2%202.395-.4s1.396-.598%201.795-.797c.997-.2%201.197-.598.997-.997z%22%2F%3E%0A%20%20%20%20%20%20%20%20%3Cpath%20fill%3D%22%2318c139%22%20fill-rule%3D%22evenodd%22%20d%3D%22M6.94%2010.77C6.543.4.16%200%20.16%200c-.4%209.374%206.78%2010.77%206.78%2010.77z%22%2F%3E%0A%20%20%20%20%3C%2Fg%3E%0A%3C%2Fsvg%3E%0A&quot;);"></jdiv></a></jdiv><jdiv class="corner_d341" style="border-left-color: rgb(37, 86, 163);"></jdiv></jdiv></jdiv></jdiv></jdiv><jdiv id="jivo-player" class="player_b2a1"><audio preload="auto" id="jivo-sound-agent_message"><source src="https://code.jivosite.com/sounds/agent_message.mp3" type="audio/mpeg"><source src="https://code.jivosite.com/sounds/agent_message.ogg" type="audio/ogg; codecs=vorbis"><source src="https://code.jivosite.com/sounds/agent_message.wav" type="audio/wav"></audio><audio preload="auto" id="jivo-sound-notification"><source src="https://code.jivosite.com/sounds/notification.mp3" type="audio/mpeg"><source src="https://code.jivosite.com/sounds/notification.ogg" type="audio/ogg; codecs=vorbis"><source src="https://code.jivosite.com/sounds/notification.wav" type="audio/wav"></audio><audio preload="auto" id="jivo-sound-outgoing_message"><source src="https://code.jivosite.com/sounds/outgoing_message.mp3" type="audio/mpeg"><source src="https://code.jivosite.com/sounds/outgoing_message.ogg" type="audio/ogg; codecs=vorbis"><source src="https://code.jivosite.com/sounds/outgoing_message.wav" type="audio/wav"></audio></jdiv><jdiv id="jcont" translate="no" class="notranslate" style="animation-duration: 300ms; animation-timing-function: cubic-bezier(0.39, 0.24, 0.21, 0.99); animation-delay: 0s; animation-iteration-count: 1; animation-direction: normal; animation-fill-mode: both; animation-play-state: running; animation-name: WidgetContainer_CLOSE_WIDGET_e75b; --jright: 39px; --jbottom: 0; --jheight: 496px; display: block; position: fixed; --jlabelwidth: 267px; --jlabelright: 0px;"><jdiv class="wrap_f13d" dir="ltr" id="jivo_action"><jdiv class="closeButton_ccd4" id="jivo_close_button"><jdiv class="closeIcon_f289" style="background-image: url(&quot;data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%2228%22%20height%3D%2228%22%20viewBox%3D%220%200%2028%2028%22%3E%0A%20%20%20%20%3Cg%20fill%3D%22none%22%20fill-rule%3D%22evenodd%22%20transform%3D%22translate(2%202)%22%3E%0A%20%20%20%20%20%20%20%20%3Ccircle%20cx%3D%2212%22%20cy%3D%2212%22%20r%3D%2212%22%20fill%3D%22%23FFF%22%20opacity%3D%221%22%2F%3E%0A%20%20%20%20%20%20%20%20%3Ccircle%20cx%3D%2212%22%20cy%3D%2212%22%20r%3D%2212.75%22%20stroke%3D%22%23222D38%22%20stroke-width%3D%221.5%22%20opacity%3D%221%22%2F%3E%0A%20%20%20%20%20%20%20%20%3Cg%20fill%3D%22%23222D38%22%20opacity%3D%221%22%20transform%3D%22translate(6%206)%22%3E%0A%20%20%20%20%20%20%20%20%20%20%20%20%3Crect%20width%3D%221.611%22%20height%3D%2213.9%22%20x%3D%225.435%22%20y%3D%22-.941%22%20rx%3D%22.806%22%20transform%3D%22rotate(45%206.24%206.01)%22%2F%3E%0A%20%20%20%20%20%20%20%20%20%20%20%20%3Crect%20width%3D%221.611%22%20height%3D%2213.9%22%20x%3D%225.435%22%20y%3D%22-.941%22%20rx%3D%22.806%22%20transform%3D%22scale(-1%201)%20rotate(45%200%20-9.058)%22%2F%3E%0A%20%20%20%20%20%20%20%20%3C%2Fg%3E%0A%20%20%20%20%3C%2Fg%3E%0A%3C%2Fsvg%3E%0A&quot;);"></jdiv></jdiv></jdiv></jdiv><jdiv class="pseudoHeight_bffa"></jdiv><jdiv class="jivoMouseTrack_e1f3"></jdiv></jdiv></jdiv></body></html>
                """;

        HashMap<String, Integer> lemmas2ranking = extractor.getLemmas2RankingFromText(extractor.removeHtmlTagsAndNormalize(html));
        System.out.println(lemmas2ranking);
        Assert.assertTrue(lemmas2ranking.get("чехол") > 1);

    }
}
