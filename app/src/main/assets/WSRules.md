# Weiß Schwarz 裁判級綜合規則與賽事判例手冊

> 這份文件由翻譯團隊整理，卡片的中文翻譯用語也依循這份規則的譯稱，回答規則問題與卡片效果問題都可以參考。

---

Weiß Schwarz 裁判級綜合規則與賽事判例手冊
執行摘要
本報告以 2026 年 9 月 4 日為基準，遊戲機制以使用者提供的日文《ヴァイスシュヴァルツ 総合ルール ver.
1.112》（2026 年 8 月 24 日更新）為核心原典，並交叉核對 Weiß Schwarz 日本官方規則頁、2026 年 9 月 1
日更新的牌組構築規則，以及 Bushiroad《TCG Advanced Floor Rules ver.1.2.12》（2026 年 7 月 9 日）的賽
事與罰則規定。日本官方目前明列綜合規則為 ver.1.112、Advanced Floor Rules 為 ver.1.2.12；日本牌組規
則頁則已更新至 ver.1.138。英文官方的 Comprehensive Rules 更新日期仍較日本版舊，因此本報告在機制差
異上優先採用最新日文原典。fileciteturn0file0 
以下中文術語是為 zh-Hant-TW 裁判實務整理的譯稱；第一次出現時盡量保留日文或英文原名，以免與各地社
群慣用語混淆。Bushiroad 公開資料顯示 WS 有日文、英文及簡體中文商品，但我沒有找到與日文 ver.1.112 同
步的官方繁體中文綜合規則，因此有爭議時應回到日文原文與該賽事公告。
裁判上最重要的幾個結論如下：
問題
裁判級結論
WS 有沒有 MTG 式的
「優先權 priority」？
沒有這個正式規則概念。 WS 使用「檢查時點（チェックタイミング）」與「行
動時點（プレイタイミング）」。玩家不能任意在對方效果中「回應」。
自動能力是不是進
stack / chain，後進先
出？
不是。 觸發後成為待機狀態；到了檢查時點，一次選一個自動能力，完整播放並
解決，再重新執行規則處理。
Mulligan 可以換幾
張？
初始手牌 5 張，因此可換 0～5 張；每位玩家只有一次換牌機會。先把要換的牌
全部放入休息室，再抽同數量，不洗回牌庫。
同時觸發誰先？
檢查時點先完成所有規則處理，再由回合玩家處理其待機自動能力，一次一個；
其待機能力清空後才輪到非回合玩家。每解決一個都重新從規則處理開始。
Clock 7 張時何時升
級？
Level Up 是中斷型規則處理，通常立即中斷目前行動；但在一項「能力費用」從
開始支付到支付完畢之間，不執行 Refresh 或 Level Up。
Refresh 何時發生？
牌庫空時是中斷型規則處理：休息室全部洗成新牌庫，然後牌庫頂 1 張放入
Clock。
攻擊中可以打幾張反
擊？
只有 Front Attack 進 Counter Step；防守玩家只有一次行動時點，最多打 1 個
具有 Counter 條件的 Event 或角色起動能力。
裁判罰則中的「Game
Loss」？
現行 Advanced Floor Rules 正式級別是 Caution、Warning、Loss of Match、
Disqualification。正式名稱是 Loss of Match，不是獨立的「Game Loss」級
別。BO1 時兩者結果通常相同，但裁判紀錄應用官方名詞。
比賽是否固定 35 分
鐘？
Floor Rules 對 WS 建議 35 分鐘，但賽事規章可以明確改變；例如 WGP2025 日
本／世界決賽預賽採 40 分鐘 BO1、決賽無時間限制。
1
2
1

---

問題
裁判級結論
官方規則未寫明怎麼
辦？
不得自行創造規則。 先區分遊戲規則、賽事規章、罰則與補救；無明文的細節由
裁判依 Floor Rules、事件資訊、可修復性與公平性裁定，Head Judge 為該賽事
最終裁定者。
上述時序、換牌、規則處理與攻擊機制直接來自 ver.1.112；賽事與罰則則由 Advanced Floor Rules 管轄。
fileciteturn0file0 
規則基礎、卡片與遊戲區域
規則適用原則。 WS 原則上是兩人對戰。任一玩家敗北時遊戲結束；若對手敗北而自己沒有敗北，即獲勝。一
般敗北條件是自己的 Level 區有 4 張以上卡，或自己的牌庫與休息室同時一張卡都沒有；一般遊戲規則下，所
有玩家同時敗北則為平手。玩家也可隨時投降，投降立即生效，不等待檢查時點，也不能被卡片效果禁止、強
迫或替代。fileciteturn0file0
卡片文字與綜合規則衝突時，卡片文字優先；不能執行的動作不執行，若只是一部分不能執行則盡可能執行可
執行部分；「禁止」效果與要求執行的效果衝突時，禁止優先。若雙方同時必須作選擇，由回合玩家先選，非
回合玩家知道其選擇後再選。這幾條是處理大量疑難題的首要裁判原則。fileciteturn0file0
但要區分「遊戲規則」與「賽事結果規則」。例如綜合規則的一般同時敗北是平手；Advanced Floor Rules 的
Single Elimination 預設規則則規定，若雙方同時滿足敗北條件，非回合玩家獲勝、回合玩家敗北，除非該比賽
另有專用規定。WGP2025 世界決賽的公告也明確採用非回合玩家勝出的同時敗北處理。
基本牌組構築。 綜合規則的底層條件為正好 50 張、同卡名合計最多 4 張、Climax 合計最多 8 張；不同版本、
卡號或能力只要卡名相同，原則上仍合計計算 4 張限制。日本官方現行主要公認／官方形式是 Neo-Standard，
即原則選擇一個 Title，僅使用該 Title 可用的卡；另有 Standard、Side Constructed 及各賽事特殊格式。2026
年後期限制表自 2026 年 6 月 27 日起適用，包含禁用、限張與「N 種選拔」等限制，因此「50／4／8」只是第
一層合法性，正式賽事還必須再檢查 Title、格式、最新禁限卡與活動專用規章。fileciteturn0file0 
三種卡片類型：
類型
主要資訊
通常如何使用
通常有效區域
角色 Character
（キャラ）
Level、Cost、Color、
Power、Soul、
Trait、Trigger、能力
Main Phase 從手牌打到
舞台；部分角色有
Backup 等可由手牌使用
的能力
印在角色上的一般能力原則
上在舞台有效；若能力明示
在其他區域運作則依文字
事件 Event（イ
ベント）
Level、Cost、Color、
文字；可有 Counter
icon
Main Phase 使用；有
Counter icon 的 Event 可
在合法 Counter Step 使
用
使用時進 Resolution Zone，
效果解決後通常進休息室
高潮 Climax（ク
ライマックス／
CX）
Color、Trigger icon、
文字
CX Phase 從手牌使用，
一次 CX Phase 最多使用
1 張
通常在 CX Zone 時文字有
效；End Phase 通常進休息
室
Climax 使用時不檢查 Level 條件，但仍受 Color 條件影響；Level 0 Character／Event 不需滿足 Color 條件。
fileciteturn0file0
3
4
5
2

---

Color 條件是欲使用的卡之顏色，必須與自己 Level 或 Clock 中至少一張卡的顏色相符；Level 條件是卡片
Level 不得高於自己的目前 Level，也就是 Level 區卡片張數。fileciteturn0file0
主要遊戲區域：
區域
公開性
順序
核心裁判事項
牌庫 Deck（山札）
非公開
固定，不能任
意改變
頂／底逐張處理；牌庫空通常立即 Refresh
手牌 Hand
非公開內容、
張數公開
自己可任意排
序
預設上限 7；End Phase 超過時丟至 7
休息室 Waiting Room
（控え室）
公開
可自由整理
已用 Event/CX、支付 Stock、離場角色等
通常來此
舞台 Stage
公開
5 個固定框位
前列 3、後列 2；一般每框 1 角色
Marker 區
原則非公開
有順序
每個舞台框位對應一個 Marker 區；宿主離
開時依規則處理 Marker
Clock
公開
不可任意改順
序
達 7 張觸發 Level Up；傷害未取消通常進
此區
Level
原則公開
不可改順序
張數就是玩家 Level；4 張以上為敗北條件
Stock
非公開、張數
公開
有順序
新 Stock 疊在上面；支付時從最上面開始
移除
CX Zone
公開
原則最多 1 張
CX Phase 使用；End Phase 通常送休息室
Memory（思い出）
原則公開
face-up 可整
理
部分 face-down Memory 依特殊規則視為
無資訊卡
Resolution Zone（解
決領域）
公開
有順序
Trigger、Damage、Event、Brainstorm
等處理的暫存區
所有區域的張數本身都是公開資訊，即使內容不是公開資訊；例如 Stock 的內容與順序不公開，但 Stock 有幾
張可確認。fileciteturn0file0
舞台幾何關係可簡化為：
自己視角
          後列
       [左]   [右]
        ↘ ↘   ↙ ↙
前列   [左] [中央] [右]
正面關係：
自己的前列左   ↔ 對手前列右
自己的前列中央 ↔ 對手前列中央
自己的前列右   ↔ 對手前列左
3

---

角色由非框位區域進入舞台框位時，除非效果另有指定，通常以 Stand 狀態進場；舞台框位之間交換或移動則
保留原本 Stand／Rest／Reverse 狀態。fileciteturn0file0
角色三種狀態為 Stand（直立）、Rest（橫置 90 度）與 Reverse（倒轉 180 度）。賽事 Floor Rules 同樣要求
狀態能清楚區辨，而不是要求精確量角度。fileciteturn0file0 
Marker。 每個舞台框位有對應 Marker 區。角色從自己的某框移至自己的另一框時，其 Marker 一併移動；若
目標框已有 Marker，相關 Marker 可能依規則全部送回各自 Owner 的休息室。角色從框位離開到非框位區域
時，原框位 Marker 通常同步進各自 Owner 的休息室。沒有角色支撐卻仍存在的 Marker，會在檢查型規則處理
中清除。fileciteturn0file0
Owner 與 Master。 Owner 是這張實體卡開始遊戲時屬於誰的牌組；Master 是目前控制該卡、能力或效果的
玩家。移動效果常指定 Owner 的區域；觸發與效果排序則多使用 Master 判定。fileciteturn0file0
開局、換牌、回合、攻擊與傷害
標準開局程序如下：玩家先提出本局使用的合法牌組；各自充分洗牌，之後讓對手有機會洗自己的牌庫；隨機
決定先攻，這個決定不可讓「擲骰贏的人再選先後攻」取代隨機先攻本身；雙方各抽 5 張；由先攻開始依序做
一次換牌；先攻成為第一回合的回合玩家。綜合規則要求起始先攻是隨機決定，而 Floor Rules 在正式賽事中更
要求洗牌充分隨機化並把牌庫交給對手作確認洗牌。fileciteturn0file0 
Mulligan／起始換牌是 WS 最常被講錯的基本規則之一。
程序是：
先攻玩家先決定一次，從初始 5 張手牌中選任意張數，把那些牌先放進休息室，然後從牌庫抽相同張數。接著
後攻玩家做同樣的一次程序。因為「任意張數」可以是 0，而最初只有 5 張，所以一般情況的換牌張數是 0、
1、2、3、4 或 5 張。換掉的牌不是洗回牌庫，看到補進來的新牌之後也沒有第二次換牌機會。
fileciteturn0file0
換牌例一：全換。 起手 5 張全部不想留：5 張全部放休息室，再抽 5。合法。
換牌例二：不換。 宣布保留全部手牌，實質換 0 張。合法，但你的這次換牌機會至此用完。
換牌例三：換三張後不滿意。 先把三張送休息室，再抽三張；即使新三張更糟，也不能再換。裁定：不得第二
次 Mulligan。
換牌例四：玩家把三張塞回牌庫後再抽。 這不是正確換牌程序；正確位置是休息室。正式比賽應立即叫裁判，
因為牌庫非公開資訊與隨機順序已可能受到污染，補救及罰則依實際可辨識性與賽事層級判斷，而不是由玩家
自行「倒回去」。
完整回合順序：
此回合架構及每個 Phase 的檢查時點均由 ver.1.112 明定。fileciteturn0file0
6
7
Stand Phase
Draw Phase
Clock Phase
Main Phase
Climax / CX Phase
Attack Phase
Encore Step
End Phase
對手成為回合玩家
4

---

Phase
強制／選擇動作
重要裁判時點
Stand
回合開始／Stand Phase 開始觸發後，將自己舞
台角色 Stand
開始觸發先進待機；檢查後再 Stand 全體
Draw
抽 1
Draw Phase 開始能力先處理，再抽
Clock
可從手牌放 1 張至 Clock；若如此做，抽 2
可選擇不 Clock；不是傷害，不能 Damage
Cancel
Main
可反覆打 Character、Event、起動能力、交換
兩個己方框位
做完一項後通常再次取得行動時點；選擇不
做則進 CX
CX
可從手牌使用 1 張 CX
此時只能打 CX；一旦執行一次行動，不再
取得第二次 CX 行動時點
Attack
依攻擊宣言→Trigger→必要時
Counter→Damage→必要時 Battle
詳見下文
Encore
將 Reverse 角色逐一送休息室並處理 Encore
每移走一名角色後都有檢查時點
End
處理 end-of-turn、手牌降至上限、CX 送休息
室、效果到期
若又產生必須處理的 End 能力或手牌超
限，End Phase 會重新循環
fileciteturn0file0
Main Phase 能做的四類基本行動是：從手牌使用 Character、從手牌使用 Event、使用合法起動能力、交換自
己兩個舞台框位中的卡。交換可以是兩名角色互換，也可以是一名角色移至另一個空框。
fileciteturn0file0
第一回合先攻限制。 先攻玩家第一回合只能完成一次 Attack Subphase；也就是第一次攻擊結束後，不能再選
第二名角色攻擊。fileciteturn0file0
一次攻擊的標準流程是：
fileciteturn0file0
攻擊方式有三種。
攻擊
條件
Soul 修正
是否
Counter
是否
Battle
Direct
Attack
正面沒有對手
角色
+1 Soul
否
否
Front
Attack
正面有角色
無固有減益
是
是
是
否
是
否
Attack Declaration
選攻擊者與攻擊方式
攻擊者Rest
Trigger Step
Front Attack？
Counter Step
Damage Step
Front Attack？
Battle Step
Attack End
下一次Attack Declaration
或結束攻擊
5

---

攻擊
條件
Soul 修正
是否
Counter
是否
Battle
Side Attack
正面有角色
正面角色 Level > 0 時，依其 Level 每級
-1 Soul
否
否
最重要的鎖定規則是：攻擊方式一旦選定，後來正面角色消失、進場或改變，不會重新判定攻擊方式，也不重
算該攻擊宣言本身產生的 Direct +1／Side -Soul。fileciteturn0file0
Trigger Step。 將己方牌庫頂 1 張表向移到 Resolution Zone，依該卡「進入 Resolution Zone 那一刻」具有
的 Trigger icon 執行；有多個 Trigger icon 時，由執行玩家自行決定其順序。一般處理完成後，該卡面朝下放
到 Stock；但 Treasure、Chance 等會先把這張 Trigger 卡移去別處，因此不再執行一般的「這張卡進
Stock」。fileciteturn0file0
Counter Step。 只有 Front Attack 才有。非回合玩家獲得一次行動時點，可以使用一項有 Counter icon 的
Event 或適用的角色起動能力，例如 Backup。執行了一項之後，不會再獲得第二次 Counter 行動時點。
fileciteturn0file0
Damage。 攻擊角色存在且 Soul 大於 0 時，對對手造成等於 Soul 的傷害。受傷玩家由牌庫頂一張一張移到
Resolution Zone：
每翻一張先看是否為 Climax。
只要這次傷害過程翻到 Climax，立即 Damage Cancel，這次傷害所翻出的全部卡送休息室。
若沒翻到 Climax，且已翻滿 N 張，全部保持其 Resolution Zone 原順序，一起放至 Clock。
把卡「直接放 Clock」不等於造成傷害，因此不會因碰到 CX 而取消。fileciteturn0file0
Battle。 只有 Front Attack。若 Attack Character 與 Defense Character 都仍存在且 Master 關係適當，較低
Power 的角色 Reverse；Power 相同則雙方都 Reverse。若其中一方已離開，則不比較 Power，也不會因這次
Battle 規則把另一方 Reverse。fileciteturn0file0
Stock、Clock、Level 三者要分開理解。
Stock 是費用資源；Trigger 的卡通常進 Stock，支付圈數 Cost 時從 Stock 最上方逐張送休息室。Stock 內容與
順序是隱藏資訊。fileciteturn0file0
Clock 同時是傷害累積與 Color 資源。Clock Phase 可以自行把手牌 1 張放 Clock 抽 2；這不是 Damage。
Clock 一達 7 張，就觸發中斷型 Level Up：從由下往上最早的那 7 張中選 1 張放入 Level，其餘 6 張以自己選的
順序送休息室。若 Clock 超過 7，處理的仍是底下 7 張。fileciteturn0file0
Level 區張數就是目前 Level；Level 0 表示沒有 Level 卡，Level 1 表示一張，如此類推。Level 區有 4 張以上即
滿足敗北條件，於下一次敗北判定規則處理時敗北。fileciteturn0file0
Refresh。 牌庫變成 0 張時，Refresh 是中斷型規則處理：立即暫停目前行動，將自己的休息室全部移到牌庫並
洗牌，然後將新牌庫頂 1 張放 Clock，之後回去繼續原本尚未完成的動作。支付一項能力 Cost 從開始到支付完
成之間是例外：Refresh 與 Level Up 暫不插入，等 Cost 支付完後再依規則處理。fileciteturn0file0
若牌庫空、休息室也空，在特定 Damage 處理狀態下可能直接敗北；其他情況則依 ver.1.112 的特殊 Refresh
條文結束該次處理。這是一個非常少見但 Head Judge 應知道的 edge case。fileciteturn0file0
1. 
2. 
3. 
4. 
6

---

效果分類、文字模板與完整時序模型
WS 的能力（ability）分成三大類，效果（effect）則分成另外三大類；這兩組分類不能混為一談。
fileciteturn0file0
層次
類型
功能
能力
永續能力 Continuous
在有效期間持續產生效果，不「播放」
能力
起動能力 Activated
玩家在自己被給予合法 Play Timing 時主動支付 Cost 使用
能力
自動能力 Automatic
誘發條件發生後進待機狀態，在後續 Check Timing 播放／解決
效果
單發效果 One-shot
執行一次即結束，如抽 1、將卡送休息室
效果
持續效果 Continuous effect
在指定期間持續修改狀態、能力、Power、Soul 等
效果
替代效果 Replacement
某事件本要發生時，以另一事件取代；原事件視為未發生
fileciteturn0file0
WS 沒有一般意義的「Priority」。 正確問法不是「現在誰有優先權？」而是「現在是否有 Check Timing？若
沒有待處理事項，規則把 Play Timing 給誰？」Main Phase 的主動 Play Timing 是回合玩家；Counter Step 是
防守方的一次特殊 Play Timing；CX Phase 是回合玩家的一次限定 Play Timing。fileciteturn0file0
更重要的是：WS 也沒有讓兩位玩家把效果疊成 LIFO stack 的一般 chain 系統。 玩家使用一項卡或能力後，它
依規則完整解決；解決中觸發的 Auto 通常只是「待機」，要等下一個 Check Timing。唯一可能真正中斷正在
處理中的事情，是像 Refresh、Level Up 這類中斷型規則處理。fileciteturn0file0
完整 Check Timing 可表示為：
7

---

因此，假設 A 與 B 同時各有三個 Auto 觸發，A 是回合玩家：A 並不是先把三個排成「chain」；A 選其中一
個，完整解決，再重新做規則處理。只要 A 仍有待機能力，A 再選一個。等 A 沒有待機能力才輪到 B。若 B 解
決一個能力後又令 A 產生新 Auto，流程回到規則處理後，A 的 Auto 又會先於 B 剩餘 Auto 處理。
fileciteturn0file0
自動能力原則上不是「可遺忘」。 誘發條件每發生一次就建立一個待機實例；到 Check Timing 時，合法的待
機 Auto 原則必須被播放。若文字含可選支付 Cost 或「可以」效果，玩家可選擇不做那個可選部分，但這個
Auto 本身仍已被播放。來源卡在 Auto 解決前離開原區域，並不會自動消除已經待機的能力。
fileciteturn0file0
注意賽事政策和遊戲規則在 missed trigger 上的差別。 Advanced Floor Rules 對「忘記處理 Automatic
Ability」規定：一般由裁判判斷是否在下一個 Check Timing 補處理；若該能力的效果本身是 “may” 型可選
擇效果，Floor Rules 視為玩家選擇不做，且該遺漏本身不給罰則。若已影響遊戲狀態，則再轉入 Illegal Game
State 分類。Level 1 預設 Caution，Level 2 以上為 Caution～Warning。
卡／能力播放程序：
宣告要播放的卡或能力；手牌卡需先合法滿足 Color／Level。
做播放時必須決定的選擇，例如 Character 要進哪個框位。
決定並支付所有 Cost。
「這張卡／能力已被播放」的事件發生。
完整解決。fileciteturn0file0
Cost 是 all-or-nothing。 如果整個 Cost 無法完整支付，就不能先付一部分再說「後面付不了」。多項 Cost 依
文字順序支付。這也正是為什麼把「先看支付 Stock 掉了什麼，再決定是否要使用」視為錯誤程序；Cost 支付
本身不能被玩家拆解成資訊優勢。Advanced Floor Rules 也特別以「先把兩張 Stock 送休息室，再宣告一張
Cost 2 手牌卡」作為不允許的步驟交換例子。fileciteturn0file0 
是
否
是
否
是
否
進入Check Timing
執行所有需要的規則處理
是否又產生規則處理？
回合玩家有待機Auto？
回合玩家選1 個
Auto，播放並完整解決
非回合玩家有待機Auto？
非回合玩家選1 個
Auto，播放並完整解決
Check Timing 結束
若規則指定，給予Play
Timing 或進下一步驟
8
1. 
2. 
3. 
4. 
5. 
9
8

---

選擇模板：
文字概念
裁定
「選 1 張」
有合法目標就必須選；不能因為不想選而選 0
「選最多 1 張／up to 1」
可合法選 0 或 1
要選 N 張但只有 M<N 張
盡可能選 M 張
非公開區域依卡片資訊搜尋
即使裁判知道實際上有符合卡，規則不保證玩家必須找到；可合法 fail
to find
「你可以做 A。若如此做，再做
B」
A 是選擇；只有實際滿足「如此做」條件才進 B
「若沒有如此做，做 C」
只有在選擇不做 A 或 A 無法依法執行的相應狀況下進 C
fileciteturn0file0
這也是為什麼「在公開的休息室選一名角色」和「從非公開牌庫找一名符合 Trait 的角色」不能用同一套「明明
有就一定要拿」邏輯。
持續效果層次。 查詢一張卡資訊時，先從印刷基礎資訊出發，再套用非 Power／Soul 的持續效果，然後套用改
變 Power／Soul 的持續效果；若效果彼此有依賴關係，被依賴者先套用；仍無法決定順序時，依效果產生的時
間先後。角色的永續能力通常以其進入目前相應框位／區域的時點判斷時間。fileciteturn0file0
替代效果。 若事件 A 被替代成 B，A 視為根本沒有發生，因此任何寫「A 發生時」的誘發都不能假定 A 曾發
生。若同一事件有多個替代效果，由受影響玩家／該事件相關 Master 依規則選適用順序，每個替代效果對同一
事件最多套用一次。攻擊方式的替代尤其有特例：回合玩家掌握的攻擊替代效果先於非回合玩家的攻擊替代效
果。fileciteturn0file0
Last Known Information，最後已知資訊。 已觸發的效果若需要查一張已離開原區域的卡，規則在適用情形
下使用其離開前最後資訊；舞台離場觸發尤其會參照離開舞台前資訊。Battle 中的「原戰鬥對手」關係也能依
規則作 LKI 參照。fileciteturn0file0
現行 Trigger icon 全表：
Trigger
執行內容
無 icon
無額外動作
Soul
本回合目前攻擊角色 Soul +1；強制
Return
可選對手角色 1 張回 Owner 手牌
Pool
可將自己牌庫頂 1 張放 Stock
Comeback
可從自己休息室取角色 1 張回手
Draw
可抽 1
Shot
建立一次時限 Auto：目前攻擊角色下一次造成的傷害若被 Cancel，給對手 1 傷害
Treasure
強制把這張 Trigger 卡本身回 Owner 手牌；之後可牌庫頂 1 張進 Stock
9

---

Trigger
執行內容
Gate
可從自己休息室取 Climax 1 張回手
Standby
可從休息室選 Level ≤ 自己目前 Level+1 的角色，以 Rest 放到任意己方框位
Choice
可從休息室選具有 Soul trigger icon 的角色 1 張，放手牌或 Stock
Chance
先強制將這張 Trigger 卡送休息室；牌庫頂最多 2 張公開，其中 1 張可進 Stock，其餘公開
卡進手牌
Discovery
公開牌庫頂最多 3 張，可選其中最多 1 名角色進手，其餘進休息室
Focus
可從休息室選 Event 或 Cost 0 以下 Character 1 張回手
Chance、Discovery 是 2026 年 1 月 ver.1.109 納入規則，Focus 是 2026 年 4 月 ver.1.110 定義；目前 ver.
1.112 均已正式包含。Discovery 有特殊細節：公開中的卡在該處理完成前仍依規則視為牌庫中的卡，因此若只
是把牌庫全部「公開」，不會在中途 Refresh；處理完使牌庫確實成空時才進 Refresh。
fileciteturn0file0
現行主要 Keyword／Keyword Ability：
Keyword
裁判核心
Alarm（アラーム）
通常要求該卡位於 Clock 最上方才產生指定能力／效果
Encore（アンコール）
舞台→休息室觸發；所有角色預設具有 Encore [③]，除非另有規定
Assist（応援）
後列時有效的持續能力
Bond（絆）
角色由手牌進舞台或「被播放而進舞台」時可付費取指定卡名；2025 年規
則有更新
Backup（助太刀）
對方 Counter Step 可由手牌用的起動能力；需滿足指定 Level
Great Performance（大活
躍）
位於前列中央且非 Reverse 時，以替代效果限制／改變對手攻擊對象
Brainstorm（集中）
指定張數由牌庫移至 Resolution，之後一併進休息室並依翻出內容計算
Change（チェンジ）
角色與指定區域的特定角色交換／變更
Memory（記憶）
參照 Memory 區的張數、資訊或自身位於 Memory
Experience（経験）
參照 Level 區卡片資訊／合計等條件
Shift（シフト）
Main Phase 開始時，在符合 Level 與同色手牌條件下交換 Clock 中該卡與
手牌
Acceleration（加速）
Cost 中包含把卡放 Clock 的能力群
Resonance（共鳴）
以公開手中特定卡作 Cost 或效果條件
Force（フォース）
特定 CX Phase 開始時付費，涉及對手角色 Stand／交換的能力群
Combination（合体）
以角色／Marker 組成指定角色的起動能力群
Separation（分離）
攻擊結束時由 Marker 形成新的角色並重組原角色／Marker
10

---

Keyword
裁判核心
Link（リンク）
本身通常無效果，是供其他卡參照的名稱型能力
Inheritance（継承）
讓自己／Marker 轉作其他舞台角色 Marker 的能力群
Turn N limit
限制該能力每回合可播放的次數；Auto 有特殊的跳過待機處理規定
除此之外還有 CX Combo icon、Replay 指示、直接跳轉 Phase／Step、額外回合等特殊規則。CX Combo
icon 本身沒有額外規則效果，只表示該能力與特定 Climax 關聯。fileciteturn0file0
無限循環。 若循環沒有任何玩家能停止，遊戲成為平手；若只有一方能停止，該玩家宣告循環次數並在之後採
取停止選項；若雙方都能停止，雙方依規則宣告次數並按較小值處理。不能利用相同完整遊戲狀態無限拖延。
fileciteturn0file0
代表性互動與逐步裁定
以下案例不依賴特定系列卡名，而是把最常見的 Judge Call 抽象成可直接套用的裁判模型；每個結果皆建立於
ver.1.112 的攻擊、Check Timing、規則處理、替代效果與 LKI 規定。fileciteturn0file0
案例 A：Direct 宣告後，正面突然出現角色。
情境：玩家 A 前列角色宣告攻擊時，正面是空框，因此是 Direct Attack；它得到 +1 Soul。之後某個 Auto 讓玩
家 B 在正面框放入角色。
裁定步驟：①宣告時正面空，因此攻擊方式已鎖成 Direct。②Direct 所給的本回合 +1 Soul 已建立。③後來出
現角色不重新判定攻擊方式。④因此沒有 Counter Step，也沒有 Battle Step。⑤攻擊角色仍按目前 Soul 傷
害。
裁定：仍是 Direct Attack，+1 Soul 保留，新角色不會與它 Battle。
案例 B：Side 宣告後，防守角色回手。
A 對 B 的 Level 2 角色 Side Attack，因此 A 的攻擊角色 -2 Soul。Trigger Step 中 Return 等效果把該防守角色
回手。
①Side 在攻擊宣告時已確定。②-2 Soul 已依當時正面角色 Level 產生。③對方角色後來離場不把 Side 變成
Direct。④也不退回原本 -2 Soul。
裁定：仍為 Side，仍吃 -2 Soul，不因空框獲得 Direct +1。 fileciteturn0file0
案例 C：Front Attack 後，攻擊角色在 Damage 前離場。
①Front 已宣告並完成 Trigger。②某 Auto 在 Damage Step 前把攻擊角色送去 Memory／手牌／休息室。
③Damage Step 檢查攻擊角色是否仍存在。④不存在則該攻擊角色不造成規則上的攻擊傷害。
裁定：不造成該次 Attack Damage；之後也因沒有完整攻守雙方而不作正常 Battle 比較。
fileciteturn0file0
案例 D：Front Attack 傷害後、防守角色在 Battle 前離場。
11

---

①Front Attack 正常造成 Damage。②Damage 後的 Auto 把 Defense Character 移走。③進 Battle Step 時只
剩 Attack Character。④Battle 規則要求雙方皆存在才比較 Power。
裁定：不比較 Power，攻擊角色不會因「原防守角色 Power 較大」而 Reverse。
案例 E：同一 Trigger 卡具有兩個 Trigger icon。
①頂牌進 Resolution Zone。②記錄它此刻擁有的所有 Trigger icon。③玩家自行決定這些 icon 的執行順序。
④之後即使該卡 icon 被其他效果增加／移除，也不改變本次已鎖定的 Trigger icon 集合。
裁定：以進 Resolution Zone 那一刻的 icon 為準，多 icon 順序由執行玩家選。 fileciteturn0file0
案例 F：Treasure 被 Trigger。
①Treasure CX 從牌庫頂進 Resolution。②Treasure 行動強制把「這張 Trigger 卡本身」回 Owner 手牌。③之
後可選把牌庫頂 1 張放 Stock。④因 Treasure 本身已離開 Resolution，Trigger Check 的普通結尾不能再把同
一張 Treasure 放 Stock。
裁定：Treasure 本身進手，不會又變成 Stock。 fileciteturn0file0
案例 G：Chance 被 Trigger。
①Chance 卡進 Resolution。②強制把這張 Chance 卡送休息室。③公開牌庫頂最多 2 張。④其中可依文字選 1
張進 Stock，其餘公開卡進手。⑤Chance 自身因已離開 Resolution，不會照一般 Trigger 結尾進 Stock。
裁定：不得把原 Chance 卡又當普通 Trigger Stock。 fileciteturn0file0
案例 H：雙方在同一事件同時各觸發兩個 Auto。
A 為回合玩家，A1/A2、B1/B2 同時待機。
①先做全部規則處理。②A 選 A1 或 A2 一個完整解決。③重新規則處理。④只要 A 還有待機 Auto，A 再選。
⑤A 的池清空後 B 才開始選 B1/B2。⑥若 B1 解決時又觸發 A3，回到規則處理後 A3 會先於 B2。
裁定：不是 A1→B1→A2→B2，也不是 stack；依 Check Timing 的回合玩家優先處理待機池規則。
fileciteturn0file0
案例 I：Auto 觸發後，來源角色已離場。
某角色有「這張卡從舞台送休息室時……」。
①角色離場事件發生。②Auto 立即成為待機狀態。③來源卡現在已在休息室，並不取消該待機實例。④Check
Timing 到來仍需播放該 Auto。⑤需要該角色離場前資訊時，依領域移動觸發／LKI 條文取得適當資訊。
裁定：來源卡離開不等於『能力消失、當作沒觸發』。 fileciteturn0file0
案例 J：Damage 中牌庫用完。
A 對 B 造成 3 Damage；B 牌庫只剩 1 張非 CX，休息室有牌。
12

---

①第一張翻進 Resolution，牌庫成 0。②Refresh 是中斷型，因此暫停本次 Damage。③休息室洗成新牌庫。
④新牌庫頂 1 張進 Clock 作 Refresh 處理。⑤若因此 Clock 達 7，Level Up 亦依法處理。⑥回到原本
Damage，繼續翻剩餘需要的牌。⑦若後續翻到 CX，本次 Damage 仍可依其本身規則 Cancel；Refresh 先前放
Clock 的那 1 張不是本次 Damage 卡，不會跟著取消。
裁定：Refresh 不會令原本 Damage 自動結束。 fileciteturn0file0
案例 K：支付 Cost 的中途 Clock 達 7。
能力 Cost 假設依文字先把一張牌放 Clock，再支付 Stock；放 Clock 後恰好達 7。
①開始支付同一項 Ability Cost。②Clock 達 7，理論上滿足 Level Up。③但規則禁止從「Cost 支付開始到全部
支付完成」之間插入 Level Up／Refresh。④先把整個合法 Cost 完成。⑤之後執行規則處理，才 Level Up。⑥
再繼續能力解決。
裁定：不能升級到新 Level 後再用新狀態支付同一 Cost 剩餘部分。 fileciteturn0file0
案例 L：「選 1 張」與「選最多 1 張」。
效果甲：「從你的休息室選 1 名角色回手。」休息室公開且至少有一名合法角色。必須選。
效果乙：「從休息室選最多 1 名角色回手。」玩家可選 0。
效果丙：「查看牌庫，找 1 名具有《X》的角色……」若這是依非公開資訊尋找特定性質卡，規則不要求玩家證
明不存在，因此可能合法找不到。
裁定：不能用『我不想選』處理公開區域的強制 choose 1；也不能用『裁判知道牌庫裡有』強迫 hidden-
zone search 一定找出。 fileciteturn0file0
案例 M：Standby 把角色放進已有角色的框。
①Standby 可以選符合 Level 條件的休息室角色。②它以 Rest 進任意己方框位，包含已有角色的框。③此時同
一框短暫有兩張 Character。④進下一次檢查型規則處理時執行重複卡處理，保留最後放入的角色，舊角色進
Owner 休息室。⑤若舊角色有 Marker，宿主離開框位還會依 Marker 規則處理。
裁定：可以用 Standby 覆蓋有角色的框；舊角色不是在 Standby 效果文字中直接『犧牲』，而是由後續規則
處理清掉。 fileciteturn0file0
案例 N：Power 3000、Assist +500，之後效果把 Power 設為 0。
CR 本身提供同型例：原角色 3000，先受既存 Assist +500 成 3500；之後播放一個「本回合把該角色 Power 變
成 0」的持續效果。兩者都是 Power 修改層，且無其他依賴關係時按效果產生時間處理，因此較晚產生的「變
0」在該例最後套用。
裁定：本回合最終 Power 為 0；隨後在 Check Timing 進行 Power 不足規則處理，Power ≤0 的 Character
送 Owner 休息室。 fileciteturn0file0
案例 O：Great Performance 與攻擊者自己的攻擊替代同時適用。
13

---

攻擊角色有「可改為對對手後列某角色 Front Attack」的攻擊替代；對手前列中央有合法 Great
Performance。
①同一攻擊事件有兩個 replacement。②攻擊方式 replacement 有專用排序：回合玩家的先處理。③先套攻擊
者「改打後列」。④再套非回合玩家 Great Performance。⑤後者再次替代這次攻擊。
裁定：最終 Front Attack Great Performance 角色。 這也是 ver.1.112 對多重 replacement 的代表例。
fileciteturn0file0
案例 P：End Phase 丟到 7 張後，又因 Auto 抽牌到 8。
①進 End 處理 end-of-turn Auto。②手牌若 >7，先降至上限。③CX Zone 卡送休息室。④Check Timing 又有
Auto，解決後抽牌到 8。⑤因 End Phase 的完成條件尚未全部滿足，End Phase 重新從規定流程執行。⑥再次
把手牌降到 7。
裁定：End Phase 不是『只檢查手牌上限一次』；ver.1.112 特別澄清了 End Phase 反覆處理。
fileciteturn0file0
案例 Q：忘記一個強制 Auto，已進下一步。
①停止遊戲、不要由玩家自行 rewind。②裁判確認它是否真的觸發、是不是 “may”。③若為強制 Auto，
Floor Rules 原則由裁判判斷是否於下一 Check Timing 處理。④若只是可選 “may” 效果，政策上視為選擇不
做，通常不因遺漏本身處罰。⑤若資訊或局面已因遺漏明顯改變，再套 Illegal Game State。
裁定：不能一概說『missed trigger 都消失』，也不能一概說『全部倒回去補』。
案例 R：多抽一張，而且已碰到手牌。
①Floor Rules 規定，從牌庫移出的牌一旦接觸手牌中的卡，即視為 Draw。②若是多抽，預設分類為 Drawing
Extra Cards。③預設罰則為 Loss of Match。④但 Tournament Level 2 以下若裁判仍能公平合法化局面，可以
採合法化補救並改給 Warning；補救必須由裁判執行，而非玩家私下把自己認為的那張牌塞回去。
裁定：立即叫裁判；『我知道是哪張，自己放回去就好』不是正式補救。
賽事程序、罰則與 Head Judge 決策模型
Advanced Floor Rules ver.1.2.12 適用於 Bushiroad 官方或認可的 WS 賽事，並明確要求高競技層級玩家熟悉
最新綜合規則、Floor Rules、Errata 與活動規章。Judge 可以在沒有玩家呼叫的情況下主動介入違規；一般
Judge 裁定可被 Head Judge 推翻，Head Judge 對該賽事的卡片與規則問題有最終裁定權。玩家對一般
Judge 裁定有疑義時可要求 Head Judge review。
裁判回答玩家問題時應提供「規則／卡片資訊」，而不是策略建議。例如「這張能力目前能不能選那張角
色？」可以回答；「我現在應該打 A 還是 B？」不應回答。官方也要求同一場賽事中的問答政策保持一致。
牌組與賽前。 Organizer／Head Judge 可要求事前 Deck Registration，也可依規定於賽事進行中或結束後要
求提交；登錄後原則不能任意改變牌組。Organizer／Head Judge 可進行 Deck Check。所有卡按最新 Errata
／最新官方文字使用，而非單純依舊卡印刷文字；官方 WS Errata 頁同樣明示修正文字連自由對戰也適用。
8
10
11
12
13
14

---

正式賽事洗牌必須充分隨機化，並在對手可見、正面資訊不被看見的情況下操作；自己的洗牌完成後必須把牌
庫提供給對手作確認洗牌。單純「發成數堆再疊回來」對隨機化效果有限，不宜把 pile/deal counting 當作唯
一洗牌方式。
Floor Rules 要求卡背／邊緣不得可辨識，正式比賽一般須使用適當牌套；損傷、圖案、方向差異若讓特定卡可
辨識，會進 marked cards/sleeves 罰則。日文卡使用翻譯紙條須取得 Organizer 或 Judge 許可，而且翻譯正
確性責任仍在玩家，不正確翻譯不會成為違規豁免。
比賽時間不是固定法定值。 Advanced Floor Rules 對 WS 的建議 game time 是 35 分鐘，但 Organizer 可以在
合理範圍明確公告其他時間。實際上 2026 年 1 月舉行的 WGP2025 日本全國決賽與世界決賽預賽均採 40 分鐘
BO1，決勝輪時間無限制。因此 Head Judge briefing 應以該活動公告為準，而不是看到 Floor Rules 的 35 分
鐘就推翻 Event Regulation。
同理，Floor Rules 的一般時間終了規則與 Annex A 只是基準；活動專用規定可以取代。若某賽事明確採用
Annex A 的 WS 決勝程序，時間到立刻停止、Judge 介入；Level 較高者敗北；Level 相同則 Clock 較多者敗
北；若兩者都一樣則繼續到下一 Check Timing，直到 Clock／Level 張數出現差異，再依規定比較。可是
WGP2025 世界決賽的預賽明定時間切れ為雙方敗北，因此該活動就不能自行改套 Annex A。
現行罰則級別
雖然玩家常說「警告、Game Loss、DQ」，現行 ver.1.2.12 的正式梯度是：
級別
用途
累積／程序
Caution
輕微、妨礙程度低的違規
同賽事第二次以上 Caution 可依層級、次數、內容升
為 Warning
Warning
中度、明顯妨礙遊戲／賽事
必須記錄；同賽事第二個 Warning 起，原則升為
Loss of Match，但 Judge 有裁量
Loss of Match
重大違規、遊戲無法合理繼
續或嚴重影響賽事
比賽中給予則該 Match 立即敗北；Judge 必須報告
Head Judge
Disqualification
影響賽事公平性、重大不當
行為／作弊等
立即失格；通常只有 Head Judge 可下 DQ，並須向
Bushiroad 報告
所以罰則不是一條機械式「第一次 Warning、第二次 Game Loss、第三次 DQ」階梯。每個違規類別有自己的
預設區間，還要看 Tournament Level、故意與否、資訊是否不可逆、影響程度、重複次數與是否能中立修復。
Tournament Level 1 原則較著重教育與一般店舖賽；Level 2 常見於大型賽事資格賽，玩家被期待熟悉規則；
Level 3 為主要高競技官方賽，即使非故意違規亦可較嚴格處理。
代表性違規與預設處理：
違規
Level 1
Level 2 以上
裁判重點
未登錄賽事中的非法牌組
Caution～Loss
of Match
Warning～Loss
of Match
先依法合法化牌組
7
14
15
16
17
18
18
15

---

違規
Level 1
Level 2 以上
裁判重點
已登錄但實際牌組不符
Caution～Loss
of Match
Warning～Loss
of Match
修正為與登錄一致；故意則轉
Foul Play
Patterned Marking，非故意
但有明顯辨識優勢
Warning～Loss
of Match
Loss of Match
更換／正常化牌套；故意則轉
作弊
傳達錯誤資訊，輕微
Caution
Caution～
Warning
尚未影響策略前即修正可不處
罰
傳達錯誤資訊，重大
Caution～
Warning
Warning～Loss
of Match
如錯報 Power 令對手額外花資
源
Illegal Game State，輕微
Caution
Caution～
Warning
能中立修正就修正
Illegal Game State，中度
Caution～
Warning
Warning
已可能影響策略，不能單純倒
帶
Illegal Game State，重大
Loss of Match
Loss of Match
原則已無法公平繼續
忘記強制 Auto
Caution
Caution～
Warning
依 Judge 判斷於下一 Check
Timing 處理
多抽牌
Loss of Match
Loss of Match
Level 2 以下在可公平合法化時
可改 Warning
看了額外牌
Caution～
Warning
Caution～
Warning
依隱藏資訊污染程度修復
慢打，輕微
Caution
Caution～
Warning
思考／選卡耗時過久
慢打，中度
Caution～
Warning
Warning～Loss
of Match
反覆無必要動作、明知剩時仍
不合理拖延
嚴重／故意 Slow Play
Loss of Match～
DQ
Loss of Match～
DQ
多次被 Judge 要求仍故意拖延
Cheating
DQ
DQ
故意取得不當優勢
故意不正移動卡片
DQ
DQ
例如趁對手沒看把休息室卡放
入其他區域
故意 Marked Cards
DQ
DQ
有意辨識特定卡
非法外部協助
Warning～DQ
Loss of Match～
DQ
Spectator 表情／一句提示也
可能構成 assistance
嚴重 Unsporting Conduct
DQ
DQ
暴力、偷竊、賄賂、賭賽果等
其中「錯誤遊戲狀態」的核心不是「倒帶越多越公平」。Floor Rules 要求 Judge 在能修復時以中立、不讓犯錯
者因此得利為前提合法化；如果中間已取得本來不應知道的新資訊，單純逐步 rewind 反而可能創造更大的優
勢。
19
20
16

---

Head Judge 現場決策流程
這與 Floor Rules 所要求的 Judge 主動維持公平、Head Judge 最終裁定及 DQ 報告制度一致。
判斷是否疑似作弊時，不能只看結果有沒有得利，也不能因玩家說「我不知道這樣算作弊」就直接排除 Foul
Play。現行 Foul Play 條文要求 Head Judge／Organizer 冷靜、客觀調查；Cheating、故意不當移牌、故意標
記等的預設罰則均為 DQ。
Judge
Call：立即停止玩家繼續動
作
保留現場：不要讓玩家自行
倒帶或洗牌
分別確認雙方陳述、公開資
訊、動作順序
辨識問題類型
純規則／卡片解釋
Illegal Game
State／程序錯誤
隱藏資訊污染／多抽／多看
行為、Slow Play、外援
疑似故意取得優勢
套最新卡文、Errata、Com
prehensive Rules
判斷可否中立合法化
套Advanced Floor Rules
類別
調查Foul Play／Cheating
宣布遊戲裁定
考量Tournament
Level、影響、重複紀錄
涉及DQ 時交Head Judge
給remedy +
penalty，清楚告知玩家
恢復比賽並補適當延長時間
21
22
17

---

真實官方執法案例
2025 年 5 月 4 日「Old School Party」曾發生玩家將休息室的卡不正地移到牌庫頂的事件。Bushiroad 將其認
定為 Advanced Floor Rules「Moving Cards Improperly」，給予該活動 DQ，並追加至 2026 年前期結束為止
的 Bushiroad 相關活動入場／參賽資格限制。這是「故意改變隱藏區域以取得優勢」應與一般誤操作分流調查
的非常清楚案例。
2025 年 12 月 27 日 WGP2025 名古屋決勝輪曾發現牌組含偽造卡，該場 Match 被判 Loss of Match；官方表示
調查後未認定玩家故意使用。2026 年 3 月另一場活動又有參加者持有偽造品而收到嚴重注意，官方並警告即使
未作為對戰作弊，也可能依情況受到活動停權等處分。這說明「非法卡／偽造品」與「故意作弊」不是自動等
號，intent 必須調查，但物件本身仍可能造成賽事處分。
FAQ、常見 Judge Call 與玩家速查
Q：對手打 Event，我可以像其他 TCG 一樣立刻打 Counter 回應嗎？
不可以。WS 沒有自由回應 stack。Event 播放後完整解決。Counter 是 Attack Phase 的特定 Counter Step，
而且只有 Front Attack 才存在。fileciteturn0file0
Q：兩個「當這張卡進場時」同時觸發，要先宣告完整順序嗎？
不用像 stack 那樣先全部排列。它們分別成為待機 Auto；到了 Check Timing，由該 Master 一次選一個播放並
完整解決。fileciteturn0file0
Q：我自己的三個 Auto 和對方兩個 Auto 同時觸發，能不能讓對方先解一個？
若你是回合玩家且你的 Auto 都合法待機，原則不能主動把處理順位讓出去；系統先處理回合玩家待機 Auto。
fileciteturn0file0
是／高度疑似
是
否
否
發現違規
有證據顯示故意取得不當優
勢？
暫停一般remedy，進行
Foul Play 調查
Head Judge 判定故意？
依對應Foul Play
類別：通常DQ
回一般違規分類
評估資訊污染與可修復性
查違規類別的default
penalty range
考量Tournament
Level、重複、影響程度
採公平remedy
口頭告知違規名稱與罰則；
需要時記錄
23
23
18

---

Q：Clock Phase 的「放 1 抽 2」是傷害嗎？可以 Cancel 嗎？
不是 Damage，不會 Cancel。只有規則或效果明確執行「造成 N 傷害」的 Damage Processing 才使用
Damage Cancel。fileciteturn0file0
Q：Refresh 的那 1 Clock 可以 Cancel 嗎？
不可以。它是 Refresh 規則處理直接把牌庫頂放 Clock，不是 Damage。fileciteturn0file0
Q：Refresh penalty 把我的 Clock 變 7，等 Damage 結束再升級嗎？
不是。Level Up 本身是中斷型規則處理，正常會在條件成立時處理，再回到原行動；能力 Cost 支付期間才有明
文延後特例。fileciteturn0file0
Q：Front Attack 時可以打兩張 Backup 嗎？
不可以。Counter Step 只給一次 Play Timing；用了一個 Event 或起動能力後，不再取得第二次。
fileciteturn0file0
Q：角色本身預設有 Encore 嗎？
有。除非另有指定，所有 Character 都有預設 Encore [③]。這是 Auto，不是「在舞台上付 3 讓它不死」；正
確事件是它舞台→休息室後觸發，再付費讓它 Rest 回原框。fileciteturn0file0
Q：我的角色 Power 因持續效果變成 0，是立即進休息室還是等 Battle？
Power≤0 是檢查型規則處理；在下一個 Check Timing 處理，不必等 Battle。fileciteturn0file0
Q：Side Attack 對手 Level 0，Soul 會 -0 嗎？
規則只有正面角色 Level 大於 0 才依 Level 減 Soul；Level 0 不減。fileciteturn0file0
Q：攻擊角色 Soul 因效果成 0 或負數呢？
Attack Damage 不發生；規則不是「造成 0 點傷害」，因此與「當此傷害被取消」等條件的互動要依「沒有
Damage 事件」理解。fileciteturn0file0
Q：我 Trigger Shot，之後攻擊角色先因某效果造成另一筆 Damage，那筆被 Cancel；Shot 等真正 Attack
Damage 再觸發嗎？
不等。Shot 追蹤的是「這個攻擊角色接下來造成的傷害」。第一筆由該角色造成的 Damage 若被 Cancel，就
會使用掉這個 Shot 的觸發機會。ver.1.112 甚至明示這不要求一定是對手承受的 Damage。
fileciteturn0file0
Q：Treasure Trigger 後，我可以選擇不要把 Treasure 拿回手嗎？
不可以；Treasure 本體回手是強制，之後的牌庫頂進 Stock 才是可選。fileciteturn0file0
Q：Standby 可以壓在原本角色上嗎？
可以。新角色 Rest 進框，原角色後續因同框多角色的規則處理被送休息室。fileciteturn0file0
Q：起手可不可以 Mulligan 6 張？
正常起手只有 5 張，所以不可能。合法範圍是 0～5 張，一次。fileciteturn0file0
Q：換掉的 CX 要不要洗回去？
不要。先送休息室，再抽同數。這也是 WS Mulligan 會直接改變早期牌庫／休息室構成的重要特色。
fileciteturn0file0
19

---

Q：忘記 optional Auto，過了幾個動作才想到，可以要求回去做嗎？
賽事政策下，若該 Automatic Ability 的效果是 “may”，一般視為選擇不做，不因該遺漏本身受罰；不能由玩
家自行要求強制倒帶。
Q：對手多抽一張但說知道是哪張，可以自己放回去嗎？
不可以自行處理。立即叫 Judge；Drawing Extra Cards 在現行 Floor Rules 預設是 Loss of Match，只有指定層
級及可公平合法化時才可能採 Warning 等補救。
Q：我不同意桌裁，可以直接和他爭辯嗎？
可以禮貌要求 Head Judge appeal；Head Judge 有推翻一般 Judge 裁定與作最終 ruling 的權限。拒絕遵守裁
定或持續與 Head Judge 爭執則可能構成 Unsporting Conduct。
Q：官方比賽就是 35 分鐘嗎？
不是。35 分鐘是 Advanced Floor Rules 的建議值；活動公告可明定 40 分鐘、無時間限制或其他方式。
WGP2025 決賽系列就是明確例子。
Q：時間到時一定比 Level／Clock 嗎？
不一定。那是 Annex A 被採用時的 WS 決勝流程之一；賽事可另定規則。WGP2025 世界決賽預賽就明定時間到
未決勝為雙方敗北。
玩家一頁速查
項目
必記
牌組
50 張；同名最多 4；CX 最多 8；另查 Title／格式／最新禁限
起手
5 張
Mulligan
一次，0～5；先送休息室，再抽同數；不洗回
先攻第一回合
最多 1 次 Attack Subphase
手牌上限
預設 7；End Phase 整理
Color
Level／Clock 中要有同色；Level 0 Character/Event 例外
Level
Level 區張數；4 張以上一般敗北
Clock
7 張時處理底部 7：1 張 Level、6 張 Waiting Room
Refresh
牌庫空：Waiting Room 洗牌→新 Deck，頂 1 到 Clock
Stock
Trigger 一般進 Stock；付費從 Stock 最上層開始
Main
Character、Event、ACT、舞台框位交換
CX
一個 CX Phase 最多打 1 張 CX
Direct
正面空；+1 Soul
Front
可 Counter；傷害後有 Battle
Side
對正面角色 Level 減 Soul；無 Counter、無 Battle
Trigger
以卡進 Resolution 那刻的 icon 為準
Counter
Front Attack 時防守方最多 1 個 Counter 行動
8
10
24
15
16
20

---

項目
必記
Damage
逐張翻；遇 CX 整筆 Cancel；否則滿 N 張進 Clock
Battle
低 Power Reverse；同 Power 雙方 Reverse
Encore
所有角色預設 Encore [③]
Auto
觸發→待機；Check Timing 一次解一個
同時 Auto
規則處理 → 回合玩家 Auto → 非回合玩家 Auto
「選 N」
能選就必須選滿，不能任意少選
「最多 N」
0～N 都合法
Cost
全部付得出才可開始；一個 Cost 不能只付一部分
Cost 中 Clock 7／Deck 0
Ability Cost 完整支付前暫不 Level Up／Refresh
卡文字 vs CR
卡文字優先
允許 vs 禁止
禁止優先
不能執行
不執行不能部分；若一項效果只部分不可能，則盡可能做能做部分
對方 Event／能力
沒有一般自由「chain 回應」
Judge Call
立即停手、保留現場、不要自行 rewind／洗牌
Appeal
可禮貌要求 Head Judge；HJ 為該賽事最終 ruling
罰則級別
Caution → Warning → Loss of Match → DQ，但不是固定逐級升等
Slow Play
即使無意也可處罰；無限時間的決勝桌仍必須合理速度
多抽／多看
不要自行放回；立即 Judge
最新文字
Errata 自動適用，舊卡仍以最新正式文字處理
此速查表的遊戲部分依 ver.1.112，賽事部分依 Advanced Floor Rules ver.1.2.12；正式比賽還必須疊加當日
Event Regulation。fileciteturn0file0 
最後必須特別保留三個「官方未固定」的裁判邊界。第一，WS 沒有官方定義的通用「priority」制度，因此不
可套其他 TCG 的優先權術語補規則。第二，發生違規後的精確 rewind 深度與個別 default range 內最終罰則，
不是綜合規則固定演算法，而需由 Judge／Head Judge 依 Floor Rules、Tournament Level、資訊污染、可修
復性及事件專用規章判斷。第三，時間限制、時間到處理、同時敗北、賽制、Deck Registration 方式及部分
牌套要求均可能由活動專用規章覆蓋一般建議；Head Judge 在開賽前應先確認當日公告，而不是只攜帶
Comprehensive Rules 就視為規則完整。
ルール/Q&A｜ヴァイスシュヴァルツ｜Weiß Schwarz
https://ws-tcg.com/rules/?utm_source=chatgpt.com
Trading Card Game｜Bushiroad Inc.
https://bushiroad.co.jp/en/business/tcg?utm_source=chatgpt.com
25
26
1
25
2
21

---

Bushiroad Floor Rules
https://en.palworld-official-cardgame.com/wordpress/wp-content/uploads/2026/07/09142501/Bushiroad-Floor-
Rules-1.2.12.pdf
デッキ構築ルール - ルール/Q&A｜ヴァイスシュヴァルツ｜Weiß Schwarz
https://ws-tcg.com/rules/deck_rule/?utm_source=chatgpt.com
罰則規定適用履歴 - ルール/Q&A｜ヴァイスシュヴァルツ｜Weiß Schwarz
https://ws-tcg.com/rules/penalty/?utm_source=chatgpt.com
3
4
6
7
8
9
10
11
12
13
14
15
16
17
18
19
20
21
22
24
26
5
23
22
