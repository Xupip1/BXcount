# BXcount

Minecraft plugin that triggers commands through count of tag


您可以使用此插件通过创建与增加玩家的标签数量来触发指令。可能需要解释一下步长和基数是什么意思，这实际上是一个等差数列，如果基数是10，步长是2，那么a1=10，d=2，但计算时不包含a1，即12 14 16 18 20 22.......这些数。

创作这个插件实际上是用来帮助我统计玩家与其他插件内容的交互次数并给予奖励。

操作指令：

/xhpycount help - 查看指令教程


基础操作：

/xhpycount add <玩家ID> <数量> <标签ID>   -  添加标签数量

/xhpycount remove <玩家ID> <数量> <标签ID>  - 减少标签数量

/xhpycount listall - 查看所有存在的标签（支持翻页）

/xhpycount list <玩家ID>  - 显示特定玩家的所有标签（支持翻页）



触发管理：

/xhpycount ct <数量> <标签ID> set <命令>  - 设置基础触发条件

/xhpycount ct <基数> <步长> <标签ID> exd <命令> - 设置周期触发条件，每次达到基数加上步长值乘N时触发

/xhpycount sps <基数> <步长> <标签ID> <命令> - 设置连续触发条件（每次超过基数加上步长值乘N时触发）

/xhpycount reduce set <数量> <标签ID>  - 删除基础触发条件

/xhpycount reduce exd - 删除周期触发条件



数据管理：

/xhpycount save  - 手动保存数据 （插件支持自动保存）

<img width="637" height="328" alt="QQ_1753186693148" src="https://github.com/user-attachments/assets/b0f85f47-e9cd-4a71-abfd-239c3b3136ff" />

<img width="649" height="310" alt="QQ_1753186709441" src="https://github.com/user-attachments/assets/9efc469d-615b-467f-83d8-44f9af70ada9" />




作者是一个新手几乎什么都不会，请见谅
