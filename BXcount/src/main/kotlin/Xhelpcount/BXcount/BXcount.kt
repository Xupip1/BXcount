package Xhelpcount.BXcount

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class BXcount : JavaPlugin() {
    // 使用JavaPlugin的logger属性

    // 存储玩家标签数量
    lateinit var playerTags: MutableMap<String, MutableMap<Int, Int>>
    // 存储触发条件
    lateinit var triggers: MutableList<TriggerCondition>
    // 记录已触发的普通条件
    lateinit var triggeredSet: MutableMap<String, MutableSet<Pair<Int, Int>>> // exd0
    // 记录已触发的周期条件
    lateinit var triggeredExd: MutableMap<String, MutableMap<Pair<Int, Int>, Int>> // exd1
    // 记录SPS触发条件 // sps新增
    lateinit var triggeredSps: MutableMap<String, MutableMap<Triple<Int, Int, Int>, Int>> // sps0

    // 数据文件
    private lateinit var dataFile: File
    private lateinit var dataConfig: YamlConfiguration

    override fun onEnable() {
        // 初始化数据文件 - 确保使用插件目录
        dataFile = File(this.dataFolder, "data.yml") // 使用this.dataFolder确保正确路径
        if (!dataFile.exists()) {
            // 自动创建目录结构
            dataFile.parentFile.mkdirs()
            dataFile.createNewFile()
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile)

        // 加载数据
        playerTags = loadPlayerTags()
        triggers = loadTriggers()
        triggeredSet = loadTriggeredSet()
        triggeredExd = loadTriggeredExd()
        triggeredSps = loadTriggeredSps() // sps新增

        // 注册命令
        val command = getCommand("xhpycount")
        if (command == null) {
            logger.severe("❌ 命令 xhpycount 注册失败！请检查 plugin.yml 配置")
            logger.severe("插件加载路径: ${this.dataFolder.parentFile.absolutePath}/")
        } else {
            command.setExecutor(CommandHandler(this))
            logger.info("✅ 命令 xhpycount 已成功注册！")
        }
    }

    override fun onDisable() {
        // 保存数据
        saveData()
    }

    // 加载玩家标签数据
    private fun loadPlayerTags(): MutableMap<String, MutableMap<Int, Int>> {
        val tags = mutableMapOf<String, MutableMap<Int, Int>>()
        if (dataConfig.contains("playerTags")) {
            val playerSection = dataConfig.getConfigurationSection("playerTags")
            playerSection?.getKeys(false)?.forEach { playerName ->
                val tagSection = playerSection.getConfigurationSection(playerName)
                tags[playerName] = tagSection?.getKeys(false)?.associate { tagIdStr ->
                    val tagId = tagIdStr.toInt()
                    val count = tagSection.getInt(tagIdStr)
                    tagId to count
                }?.toMutableMap() ?: mutableMapOf()
            }
        }
        return tags
    }

    // 保存玩家标签数据
    private fun savePlayerTags() {
        dataConfig.set("playerTags", null) // 清空旧数据
        playerTags.forEach { (playerName, tagMap) ->
            tagMap.forEach { (tagId, count) ->
                dataConfig.set("playerTags.$playerName.$tagId", count)
            }
        }
    }

    // 加载触发条件
    private fun loadTriggers(): MutableList<TriggerCondition> {
        val triggerList = mutableListOf<TriggerCondition>()
        if (dataConfig.contains("triggers")) {
            val triggers = dataConfig.getList("triggers")
            triggers?.forEach { trigger ->
                @Suppress("UNCHECKED_CAST")
                val triggerMap = trigger as? Map<String, Any> ?: return@forEach

                when (triggerMap["type"] as? String) {
                    "SetCondition" -> {
                        triggerList.add(TriggerCondition.SetCondition(
                            triggerMap["targetAmount"] as? Int ?: return@forEach,
                            triggerMap["tagId"] as? Int ?: return@forEach,
                            triggerMap["command"] as? String ?: return@forEach
                        ))
                    }
                    "ExdCondition" -> {
                        triggerList.add(TriggerCondition.ExdCondition(
                            triggerMap["base"] as? Int ?: return@forEach,
                            triggerMap["step"] as? Int ?: return@forEach,
                            triggerMap["tagId"] as? Int ?: return@forEach,
                            triggerMap["command"] as? String ?: return@forEach
                        ))
                    }
                    "SpsCondition" -> { // sps新增
                        triggerList.add(TriggerCondition.SpsCondition(
                            triggerMap["base"] as? Int ?: return@forEach,
                            triggerMap["step"] as? Int ?: return@forEach,
                            triggerMap["tagId"] as? Int ?: return@forEach,
                            triggerMap["command"] as? String ?: return@forEach
                        ))
                    }
                }
            }
        }
        return triggerList
    }

    // 保存触发条件
    private fun saveTriggers() {
        dataConfig.set("triggers", null) // 清空旧数据

        val triggerList = triggers.map { trigger ->
            when (trigger) {
                is TriggerCondition.SetCondition -> mapOf(
                    "type" to "SetCondition",
                    "targetAmount" to trigger.targetAmount,
                    "tagId" to trigger.tagId,
                    "command" to trigger.command
                )
                is TriggerCondition.ExdCondition -> mapOf(
                    "type" to "ExdCondition",
                    "base" to trigger.base,
                    "step" to trigger.step,
                    "tagId" to trigger.tagId,
                    "command" to trigger.command
                )
                is TriggerCondition.SpsCondition -> mapOf( // sps新增
                    "type" to "SpsCondition",
                    "base" to trigger.base,
                    "step" to trigger.step,
                    "tagId" to trigger.tagId,
                    "command" to trigger.command
                )
            }
        }

        dataConfig.set("triggers", triggerList)
    }

    // 加载单次触发记录
    private fun loadTriggeredSet(): MutableMap<String, MutableSet<Pair<Int, Int>>> {
        val triggered = mutableMapOf<String, MutableSet<Pair<Int, Int>>>()
        if (dataConfig.contains("triggeredSet")) {
            val triggerSection = dataConfig.getConfigurationSection("triggeredSet")
            triggerSection?.getKeys(false)?.forEach { playerName ->
                val playerSection = triggerSection.getConfigurationSection(playerName)
                playerSection?.getKeys(false)?.forEach { tagIdStr ->
                    val tagId = tagIdStr.toInt()
                    val amountList = playerSection.getList(tagIdStr) as? List<Int> ?: emptyList()
                    amountList.forEach { amount ->
                        triggered
                            .getOrPut(playerName) { mutableSetOf() }
                            .add(Pair(tagId, amount))
                    }
                }
            }
        }
        return triggered
    }

    // 保存单次触发记录
    private fun saveTriggeredSet() {
        dataConfig.set("triggeredSet", null) // 清空旧数据

        triggeredSet.forEach { (playerName, records) ->
            records.forEach { (tagId, amount) ->
                dataConfig.set(
                    "triggeredSet.$playerName.$tagId.$amount",
                    true
                )
            }
        }
    }

    // 加载周期触发记录
    private fun loadTriggeredExd(): MutableMap<String, MutableMap<Pair<Int, Int>, Int>> {
        val triggered = mutableMapOf<String, MutableMap<Pair<Int, Int>, Int>>()
        if (dataConfig.contains("triggeredExd")) {
            val triggerSection = dataConfig.getConfigurationSection("triggeredExd")
            triggerSection?.getKeys(false)?.forEach { playerName ->
                val playerSection = triggerSection.getConfigurationSection(playerName)
                playerSection?.getKeys(false)?.forEach { tagIdStr ->
                    val tagId = tagIdStr.toInt()
                    val tagSection = playerSection.getConfigurationSection(tagIdStr)
                    tagSection?.getKeys(false)?.forEach { baseStr ->
                        val base = baseStr.toInt()
                        val count = tagSection.getInt(baseStr)
                        triggered
                            .getOrPut(playerName) { mutableMapOf() }
                            .set(Pair(tagId, base), count)
                    }
                }
            }
        }
        return triggered
    }

    // 保存周期触发记录
    private fun saveTriggeredExd() {
        dataConfig.set("triggeredExd", null) // 清空旧数据

        triggeredExd.forEach { (playerName, records) ->
            records.forEach { (pair, count) ->
                dataConfig.set(
                    "triggeredExd.$playerName.${pair.first}.${pair.second}",
                    count
                )
            }
        }
    }

    // 加载sp触发记录
    private fun loadTriggeredSps(): MutableMap<String, MutableMap<Triple<Int, Int, Int>, Int>> { // sps新增
        val triggered = mutableMapOf<String, MutableMap<Triple<Int, Int, Int>, Int>>()
        if (dataConfig.contains("triggeredSps")) {
            val triggerSection = dataConfig.getConfigurationSection("triggeredSps")
            triggerSection?.getKeys(false)?.forEach { playerName ->
                val playerSection = triggerSection.getConfigurationSection(playerName)
                playerSection?.getKeys(false)?.forEach { baseStr ->
                    val base = baseStr.toInt()
                    val baseSection = playerSection.getConfigurationSection(baseStr)
                    baseSection?.getKeys(false)?.forEach { stepStr ->
                        val step = stepStr.toInt()
                        val stepSection = baseSection.getConfigurationSection(stepStr)
                        stepSection?.getKeys(false)?.forEach { tagIdStr ->
                            val tagId = tagIdStr.toInt()
                            val count = stepSection.getInt(tagIdStr)
                            triggered
                                .getOrPut(playerName) { mutableMapOf() }
                                .set(Triple(base, step, tagId), count)
                        }
                    }
                }
            }
        }
        return triggered
    }

    // 保存sp触发记录
    private fun saveTriggeredSps() { // sps新增
        dataConfig.set("triggeredSps", null) // 清空旧数据

        triggeredSps.forEach { (playerName, records) ->
            records.forEach { (key, count) ->
                dataConfig.set(
                    "triggeredSps.$playerName.${key.first}.${key.second}.${key.third}",
                    count
                )
            }
        }
    }

    // 保存所有数据
    fun saveData() {
        savePlayerTags()
        saveTriggers()
        saveTriggeredSet()
        saveTriggeredExd()
        saveTriggeredSps() // sps新增

        try {
            dataConfig.save(dataFile)
        } catch (e: Exception) {
            logger.severe("无法保存数据文件: $e")
        }
    }
}

// 触发条件基类
sealed class TriggerCondition {
    data class SetCondition(
        val targetAmount: Int,
        val tagId: Int,
        val command: String
    ) : TriggerCondition()

    data class ExdCondition(
        val base: Int,
        val step: Int,
        val tagId: Int,
        val command: String
    ) : TriggerCondition()

    data class SpsCondition( // sps新增
        val base: Int,
        val step: Int,
        val tagId: Int,
        val command: String
    ) : TriggerCondition()
}

class CommandHandler(private val plugin: BXcount) : CommandExecutor {
    // 每页显示条目数
    private val ENTRIES_PER_PAGE = 5

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        // 添加OP权限检查
        if (!sender.isOp) {
            sender.sendMessage("§x§e§b§a§0§a§c错误: 你需要有管理员权限才能执行此命令")
            return false
        }
        
        val result = when (args.getOrNull(0)?.lowercase()) {
            "add" -> handleAdd(sender, args)
            "remove" -> handleRemove(sender, args)
            "ct" -> handleTrigger(sender, args)
            "save" -> handleSave(sender)
            "list" -> handleList(sender, args)
            "listall" -> handleListAll(sender, args)
            "create" -> handleCreate(sender, args)
            "delete" -> handleDelete(sender, args)
            "reduce" -> handleReduce(sender, args)
            "sps" -> handleSps(sender, args)
            "help" -> {
                sendHelp(sender)
                true
            }
            else -> {
                sender.sendMessage("§x§e§b§a§0§a§c未知命令. 输入 /xhpycount help 查看帮助")
                true
            }
        }
        return result
    }

    private fun handleList(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size < 2) {
            sender.sendMessage("§x§e§b§a§0§a§c请指定玩家名")
            sender.sendMessage("§x§f§9§e§2§a§f用法: /xhpycount list <玩家名> [页码]")
            return false
        }

        // 获取玩家名和页码
        val playerName = args[1]
        var page = 1
        if (args.size >= 3) {
            val pageNumStr = args[2]
            if (pageNumStr.toIntOrNull() != null) {
                page = pageNumStr.toInt()
            } else {
                sender.sendMessage("§x§e§b§a§0§a§c页码必须为有效数字")
                sender.sendMessage("§x§f§9§e§2§a§f用法: /xhpycount list $playerName <页码>")
                return false
            }
        }

        // 显示特定玩家的标签
        val playerTags = plugin.playerTags[playerName] ?: run {
            sender.sendMessage("§x§e§b§a§0§a§c玩家 $playerName 没有任何标签")
            return false
        }

        // 按标签ID排序
        val sortedTags = playerTags.toSortedMap()

        // 分页计算
        val totalPages = (sortedTags.size - 1) / ENTRIES_PER_PAGE + 1

        if (page < 1 || page > totalPages) {
            sender.sendMessage("§x§e§b§a§0§a§c无效的页码. 有效范围: 1-$totalPages")
            sender.sendMessage("§x§f§9§e§2§a§f用法: /xhpycount list $playerName <页码>")
            return false
        }

        val entries = sortedTags.entries.toList()
        val start = (page - 1) * ENTRIES_PER_PAGE
        val end = minOf(start + ENTRIES_PER_PAGE, entries.size)

        sender.sendMessage("§x§a§6§e§3§a§1玩家 $playerName 的标签信息 (第$page/$totalPages):")
        sender.sendMessage("§x§f§9§e§2§a§f使用 §x§a§6§e§3§a§1/xhpycount list $playerName <页码> §x§f§9§e§2§a§f翻页")
        for (i in start until end) {
            val (tagId, count) = entries[i]
            sender.sendMessage("§x§f§9§e§2§a§f标签ID: §x§a§6§e§3§a§1$tagId §x§f§9§e§2§a§f数量: §x§a§6§e§3§a§1$count")
        }
        return true
    }

    private fun handleListAll(sender: CommandSender, args: Array<String>): Boolean {
        // 解析页码参数（默认第一页）
        var page = 1
        var playerName = "" // 新增变量存储玩家名
        if (args.size >= 2) {
            val pageNumStr = args[1]
            if (pageNumStr.toIntOrNull() != null) {
                page = pageNumStr.toInt()
            } else {
                sender.sendMessage("§x§e§b§a§0§a§c页码必须为有效数字")
                sender.sendMessage("§x§f§9§e§2§a§f用法: /xhpycount listall <页码>")
                return false
            }
        }

        // 显示所有标签
        val allTags = mutableSetOf<Int>()
        plugin.playerTags.values.forEach { allTags.addAll(it.keys) }

        if (allTags.isEmpty()) {
            sender.sendMessage("§x§e§b§a§0§a§c当前没有创建任何标签")
            return false
        }

        // 按标签ID排序
        val sortedTags = allTags.sorted()

        // 分页计算
        val totalPages = (sortedTags.size - 1) / ENTRIES_PER_PAGE + 1

        if (page < 1 || page > totalPages) {
            sender.sendMessage("§x§e§b§a§0§a§c无效的页码. 有效范围: 1-$totalPages")
            sender.sendMessage("§x§f§9§e§2§a§f用法: /xhpycount listall <页码>")
            return false
        }

        val start = (page - 1) * ENTRIES_PER_PAGE
        val end = minOf(page * ENTRIES_PER_PAGE, sortedTags.size)

        sender.sendMessage("§x§a§6§e§3§a§1当前存在的所有标签 (第$page/$totalPages):")
        sender.sendMessage("§x§f§9§e§2§a§f使用 §x§b§a§c§2§d§e/xhpycount list $playerName <页码> §x§f§9§e§2§a§f翻页")
        for (i in start until end) {
            sender.sendMessage("§x§f§9§e§2§a§f- §x§a§6§e§3§a§1${sortedTags[i]}")
        }
        return true
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§x§a§6§e§3§a§1================ §x§7§4§c§7§e§cBXCount插件使用指南 §x§a§6§e§3§a§1================")
        sender.sendMessage("§x§a§6§e§3§a§1基本功能说明:")
        sender.sendMessage("  §x§f§9§e§2§a§f这个插件可以帮助你通过标签数量的方式来触发指令，更加适合辅助其他插件")
        sender.sendMessage("")


        sender.sendMessage("§x§b§a§c§2§d§e1. §x§a§6§e§3§a§1基础操作")
        sender.sendMessage("  §x§f§9§e§2§a§f/xhpycount add <玩家ID> <数量> <标签ID> §x§b§a§c§2§d§e- 添加标签数量")
        sender.sendMessage("  §x§f§9§e§2§a§f/xhpycount remove <玩家ID> <数量> <标签ID> §x§b§a§c§2§d§e- 减少标签数量")
        sender.sendMessage("  §x§f§9§e§2§a§f/xhpycount create <标签ID> §x§b§a§c§2§d§e- 创建新标签，这个属实没啥用到时候会修")
        sender.sendMessage("  §x§f§9§e§2§a§f/xhpycount delete <标签ID> §x§b§a§c§2§d§e- 删除指定标签")
        sender.sendMessage("  §x§f§9§e§2§a§f/xhpycount listall §x§b§a§c§2§d§e- 查看所有存在的标签（支持翻页）")
        sender.sendMessage("  §x§f§9§e§2§a§f/xhpycount list <玩家ID> §x§b§a§c§2§d§e- 显示特定玩家的所有标签（支持翻页）")
        sender.sendMessage("")

        sender.sendMessage("§x§b§a§c§2§d§e2. §x§a§6§e§3§a§1触发条件管理")
        sender.sendMessage("  §x§f§9§e§2§a§f/xhpycount ct <数量> <标签ID> set <命令> §x§b§a§c§2§d§e- 设置基础触发条件")
        sender.sendMessage("  §x§f§9§e§2§a§f/xhpycount ct <基数> <步长> <标签ID> exd <命令> §x§b§a§c§2§d§e- 设置周期触发条件，每次达到基数加上步长值乘N时触发")
        sender.sendMessage("  §x§f§9§e§2§a§f/xhpycount sps <基数> <步长> <标签ID> <命令> §x§b§a§c§2§d§e- 设置连续触发条件（每次超过基数加上步长值乘N时触发）")
        sender.sendMessage("  §x§f§9§e§2§a§f/xhpycount reduce set <数量> <标签ID> §x§b§a§c§2§d§e- 删除基础触发条件")
        sender.sendMessage("  §x§f§9§e§2§a§f/xhpycount reduce exd <基数> <步长> <标签ID> §x§b§a§c§2§d§e- 删除周期触发条件")
        sender.sendMessage("")

        sender.sendMessage("§x§b§a§c§2§d§e3. §x§a§6§e§3§a§1数据管理")
        sender.sendMessage("  §x§f§9§e§2§a§f/xhpycount save §x§b§a§c§2§d§e- 手动保存数据")
        sender.sendMessage("")

        sender.sendMessage("§x§a§6§e§3§a§1示例用法演示:")
        sender.sendMessage("  §x§f§a§b§3§8§7# 添加100个标签1给玩家Notch")
        sender.sendMessage("  §x§f§9§e§2§a§f/xhpycount add Notch 100 1")
        sender.sendMessage("  ")
        sender.sendMessage("  §x§f§a§b§3§8§7# 查看所有标签的第2页")
        sender.sendMessage("  §x§f§9§e§2§a§f/xhpycount listall 2")
        sender.sendMessage("  ")
        sender.sendMessage("  §x§f§a§b§3§8§7# 查看玩家Notch的标签第3页")
        sender.sendMessage("  §x§f§9§e§2§a§f/xhpycount list Notch 3")
        sender.sendMessage("§x§a§6§e§3§a§1=====================================================")
    }

    private fun handleAdd(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size < 4) {
            sender.sendMessage("§x§e§b§a§0§a§c参数不足！正确用法: /xhpycount add <玩家ID> <数量> <标签ID>")
            return false
        }

        val playerName = args[1]
        val amount = args[2].toIntOrNull() ?: run {
            sender.sendMessage("§x§e§b§a§0§a§c数量必须为数字")
            return false
        }
        val tagId = args[3].toIntOrNull() ?: run {
            sender.sendMessage("§x§e§b§a§0§a§c标签ID必须为数字")
            return false
        }

        val tags = plugin.playerTags.getOrPut(playerName) { mutableMapOf() }
        tags[tagId] = tags.getOrDefault(tagId, 0) + amount
        sender.sendMessage("§x§a§6§e§3§a§1已为玩家 $playerName 添加 $amount 个标签 $tagId")

        // 检查触发条件
        checkTriggers(playerName, tagId, tags[tagId]!!)
        return true
    }

    private fun handleRemove(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size < 4) {
            sender.sendMessage("§x§e§b§a§0§a§c参数不足！正确用法: /xhpycount remove <玩家ID> <数量> <标签ID>")
            return false
        }

        val playerName = args[1]
        val amount = args[2].toIntOrNull() ?: run {
            sender.sendMessage("§x§e§b§a§0§a§c数量必须为数字")
            return false
        }
        val tagId = args[3].toIntOrNull() ?: run {
            sender.sendMessage("§x§e§b§a§0§a§c标签ID必须为数字")
            return false
        }

        val tags = plugin.playerTags.getOrPut(playerName) { mutableMapOf() }
        val current = tags.getOrDefault(tagId, 0)
        val newAmount = maxOf(0, current - amount)
        tags[tagId] = newAmount
        sender.sendMessage("§x§a§6§e§3§a§1已为玩家 $playerName 减少 $amount 个标签 $tagId")

        // 检查触发条件
        checkTriggers(playerName, tagId, newAmount)
        return true
    }

    private fun handleTrigger(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size < 5) {
            sender.sendMessage("§x§e§b§a§0§a§c参数不足！示例用法:")
            sender.sendMessage("§x§f§9§e§2§a§f/xhpycount ct 5 100 set gamemode nameid creative")
            sender.sendMessage("§x§f§9§e§2§a§f/xhpycount ct 10 2 100 exd money give nameid 50")
            return false
        }

        when {
            args.size >= 4 && args[3].lowercase() == "set" -> {
                if (args.size < 5) {
                    sender.sendMessage("§x§e§b§a§0§a§c参数不足！set触发需要目标数量 标签ID")
                    return false
                }
                val targetAmount = args[1].toInt()
                val tagId = args[2].toInt()
                val cmdArgs = args.copyOfRange(4, args.size)
                val command = cmdArgs.joinToString(" ")

                plugin.triggers.add(
                    TriggerCondition.SetCondition(targetAmount, tagId, command)
                )
                sender.sendMessage("§x§a§6§e§3§a§1已设置基础触发条件: 标签ID=$tagId 数量=$targetAmount")
                return true
            }
            args.size >= 5 && args[4].lowercase() == "exd" -> {
                if (args.size < 6) {
                    sender.sendMessage("§x§e§b§a§0§a§c参数不足！exd触发需要基数 步长 标签ID")
                    return false
                }
                val base = args[1].toInt()
                val step = args[2].toInt()
                val tagId = args[3].toInt()
                val cmdArgs = args.copyOfRange(5, args.size)
                val command = cmdArgs.joinToString(" ")

                plugin.triggers.add(
                    TriggerCondition.ExdCondition(base, step, tagId, command)
                )
                sender.sendMessage("§x§a§6§e§3§a§1已设置周期触发条件: 标签ID=$tagId 基数=$base 步长=$step")
                return true
            }
            else -> {
                sender.sendMessage("§x§e§b§a§0§a§c未知触发类型. 请使用 set 或 exd")
                sender.sendMessage("§x§f§9§e§2§a§f示例用法:")
                sender.sendMessage("§x§f§9§e§2§a§f/xhpycount ct 10 2 2 exd money give nameid 50")
                return false
            }
        }
    }

    private fun handleSave(sender: CommandSender): Boolean {
        plugin.saveData()
        sender.sendMessage("§x§a§6§e§3§a§1数据已手动保存")
        return true
    }

    private fun handleCreate(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size < 2) {
            sender.sendMessage("§x§e§b§a§0§a§c参数不足！正确用法: /xhpycount create <标签ID>")
            return false
        }

        val tagId = args[1].toIntOrNull() ?: run {
            sender.sendMessage("§x§e§b§a§0§a§c标签ID必须为数字")
            return false
        }

        plugin.playerTags.forEach { (_, tags) ->
            if (tags.containsKey(tagId)) {
                sender.sendMessage("§x§e§b§a§0§a§c标签ID $tagId 已存在")
                return false
            }
        }

        sender.sendMessage("§x§a§6§e§3§a§1已创建标签ID $tagId")
        return true
    }

    private fun handleDelete(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size < 2) {
            sender.sendMessage("§x§e§b§a§0§a§c参数不足！正确用法: /xhpycount delete <标签ID>")
            return false
        }

        val tagId = args[1].toIntOrNull() ?: run {
            sender.sendMessage("§x§e§b§a§0§a§c标签ID必须为数字")
            return false
        }

        plugin.playerTags.forEach { (_, tags) ->
            if (tags.containsKey(tagId)) {
                tags.remove(tagId)
                sender.sendMessage("§x§a§6§e§3§a§1已删除标签ID $tagId")
                return true
            }
        }

        sender.sendMessage("§x§e§b§a§0§a§c标签ID $tagId 不存在")
        return false
    }

    private fun handleReduce(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size < 4) {
            sender.sendMessage("§x§e§b§a§0§a§c参数不足！正确用法: /xhpycount reduce <数量> <标签ID>")
            return false
        }

        val targetAmount = args[1].toIntOrNull() ?: run {
            sender.sendMessage("§x§e§b§a§0§a§c数量必须为数字")
            return false
        }
        val tagId = args[2].toIntOrNull() ?: run {
            sender.sendMessage("§x§e§b§a§0§a§c标签ID必须为数字")
            return false
        }

        plugin.triggers.removeIf { condition ->
            when (condition) {
                is TriggerCondition.SetCondition -> {
                    condition.tagId == tagId && condition.targetAmount == targetAmount
                }
                is TriggerCondition.ExdCondition -> {
                    condition.tagId == tagId && condition.base == targetAmount
                }
                else -> false
            }
        }

        sender.sendMessage("§x§a§6§e§3§a§1已删除触发条件: 标签ID=$tagId 数量=$targetAmount")
        return true
    }

    private fun handleSps(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size < 6) {
            sender.sendMessage("§x§e§b§a§0§a§c参数不足！示例用法:")
            sender.sendMessage("§x§f§9§e§2§a§f/xhpycount sps 10 2 5 give Xupipi diamond 3")
            sender.sendMessage("§x§a§6§e§3§a§1说明: 当玩家标签数量超过基数后，每增加步长值触发一次命令，@count代表触发次数，@value代表当前值")
            return false
        }

        val base = args[1].toIntOrNull() ?: run {
            sender.sendMessage("§x§e§b§a§0§a§c基数必须为数字")
            return false
        }
        val step = args[2].toIntOrNull() ?: run {
            sender.sendMessage("§x§e§b§a§0§a§c步长必须为数字")
            return false
        }
        val tagId = args[3].toIntOrNull() ?: run {
            sender.sendMessage("§x§e§b§a§0§a§c标签ID必须为数字")
            return false
        }
        val cmdArgs = args.copyOfRange(4, args.size)
        val command = cmdArgs.joinToString(" ")

        plugin.triggers.add(
            TriggerCondition.SpsCondition(base, step, tagId, command)
        )
        sender.sendMessage("§x§a§6§e§3§a§1已设置SPS触发条件: 标签ID=$tagId 基数=$base 步长=$step")
        return true
    }

    // 使用JavaPlugin的logger属性
    private fun checkTriggers(playerName: String, tagId: Int, currentAmount: Int): Boolean {
        return try {
            plugin.triggers.forEach { condition ->
                when (condition) {
                    is TriggerCondition.SetCondition -> {
                        if (condition.tagId == tagId && currentAmount == condition.targetAmount) {
                            if (!hasTriggered(playerName, condition)) {
                                executeCommand(condition.command, playerName)
                                recordTrigger(playerName, condition)
                            }
                        }
                    }
                    is TriggerCondition.ExdCondition -> {
                        if (condition.tagId == tagId && currentAmount >= condition.base) {
                            val diff = currentAmount - condition.base
                            if (diff >= 0 && diff % condition.step == 0) {
                                val triggerCount = diff / condition.step
                                if (triggerCount > getExdTriggerCount(playerName, condition)) {
                                    executeCommand(condition.command, playerName)
                                    recordExdTrigger(playerName, condition, triggerCount)
                                }
                            }
                        }
                    }
                    is TriggerCondition.SpsCondition -> {
                        if (condition.tagId == tagId && currentAmount > condition.base) {
                            val diff = currentAmount - condition.base
                            val maxTriggerCount = diff / condition.step
                            if (maxTriggerCount > 0) {
                                val lastTriggered = getSpsTriggerCount(playerName, condition)
                                val triggerTimes = maxTriggerCount - lastTriggered
                                if (triggerTimes > 0) {
                                    // 修改为循环触发
                                    for (i in 0 until triggerTimes) {
                                        val triggeredValue = condition.base + (lastTriggered + i + 1) * condition.step
                                        val cmdWithCount = condition.command.replace("@count", (i + 1).toString())
                                        val cmdWithFinalValue = cmdWithCount.replace("@value", (triggeredValue).toString())
                                        executeCommand(cmdWithFinalValue, playerName)
                                    }
                                    recordSpsTrigger(playerName, condition, maxTriggerCount)
                                }
                            }
                        }
                    }
                    else -> { /* 添加默认分支处理未知类型 */ }
                }
            }
            true
        } catch (e: Exception) {
            plugin.logger.severe("§x§e§b§a§0§a§c触发检查异常: $e")
            false
        }
    }

    private fun hasTriggered(playerName: String, condition: TriggerCondition.SetCondition): Boolean {
        return plugin.triggeredSet.getOrPut(playerName) { mutableSetOf() }
            .contains(Pair(condition.tagId, condition.targetAmount))
    }

    private fun recordTrigger(playerName: String, condition: TriggerCondition.SetCondition) {
        plugin.triggeredSet.getOrPut(playerName) { mutableSetOf() }
            .add(Pair(condition.tagId, condition.targetAmount))
    }

    private fun getExdTriggerCount(playerName: String, condition: TriggerCondition.ExdCondition): Int {
        return plugin.triggeredExd.getOrPut(playerName) { mutableMapOf() }
            .getOrDefault(Pair(condition.tagId, condition.base), 0)
    }

    private fun recordExdTrigger(
        playerName: String,
        condition: TriggerCondition.ExdCondition,
        count: Int
    ) {
        plugin.triggeredExd.getOrPut(playerName) { mutableMapOf() }
            .set(Pair(condition.tagId, condition.base), count)
    }

    private fun getSpsTriggerCount(playerName: String, condition: TriggerCondition.SpsCondition): Int {
        return plugin.triggeredSps.getOrPut(playerName) { mutableMapOf() }
            .getOrDefault(Triple(condition.base, condition.step, condition.tagId), 0)
    }

    private fun recordSpsTrigger(
        playerName: String,
        condition: TriggerCondition.SpsCondition,
        count: Int
    ) {
        plugin.triggeredSps.getOrPut(playerName) { mutableMapOf() }
            .set(Triple(condition.base, condition.step, condition.tagId), count)
    }

    private fun executeCommand(template: String, playerName: String) {
        val command = template.replace("nameid", playerName)
        plugin.server.dispatchCommand(plugin.server.consoleSender, command)
    }
}