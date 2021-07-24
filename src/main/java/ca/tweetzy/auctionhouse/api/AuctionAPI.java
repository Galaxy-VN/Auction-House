package ca.tweetzy.auctionhouse.api;

import ca.tweetzy.auctionhouse.AuctionHouse;
import ca.tweetzy.auctionhouse.auction.AuctionItem;
import ca.tweetzy.auctionhouse.auction.AuctionSaleType;
import ca.tweetzy.auctionhouse.helpers.ConfigurationItemHelper;
import ca.tweetzy.auctionhouse.settings.Settings;
import ca.tweetzy.core.compatibility.XMaterial;
import ca.tweetzy.core.utils.TextUtils;
import ca.tweetzy.core.utils.items.ItemUtils;
import ca.tweetzy.core.utils.nms.NBTEditor;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.awt.*;
import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The current file has been created by Kiran Hart
 * Date Created: January 17 2021
 * Time Created: 6:10 p.m.
 * Usage of any code found within this class is prohibited unless given explicit permission otherwise
 */
public class AuctionAPI {

    private static AuctionAPI instance;

    private AuctionAPI() {
    }

    public static AuctionAPI getInstance() {
        if (instance == null) {
            instance = new AuctionAPI();
        }
        return instance;
    }

    /**
     * @param value a long number to be converted into a easily readable text
     * @return a user friendly number to read
     */
    public String getFriendlyNumber(double value) {
        int power;
        String suffix = " KMBTQ";
        String formattedNumber = "";

        NumberFormat formatter = new DecimalFormat("#,###.#");
        power = (int) StrictMath.log10(value);
        value = value / (Math.pow(10, (power / 3) * 3));
        formattedNumber = formatter.format(value);
        formattedNumber = formattedNumber + suffix.charAt(power / 3);
        return formattedNumber.length() > 4 ? formattedNumber.replaceAll("\\.[0-9]+", "") : formattedNumber;
    }

    /**
     * Used to convert seconds to days, hours, minutes, and seconds
     *
     * @param seconds is the amount of seconds to convert
     * @return an array containing the total number of days, hours, minutes, and seconds remaining
     */
    public long[] getRemainingTimeValues(long seconds) {
        long[] vals = new long[4];
        int day = (int) TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) - (day * 24L);
        long minute = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);
        long second = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) * 60);
        vals[0] = day;
        vals[1] = hours;
        vals[2] = minute;
        vals[3] = second;
        return vals;
    }

    /**
     * Used to convert milliseconds (usually System.currentMillis) into a readable date format
     *
     * @param milliseconds is the total milliseconds
     * @return a readable date format
     */
    public String convertMillisToDate(long milliseconds) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Settings.DATE_FORMAT.getString());
        Date date = new Date(milliseconds);
        return simpleDateFormat.format(date);
    }

    /**
     * Used to convert a serializable object into a base64 string
     *
     * @param object is the class that implements Serializable
     * @return the base64 encoded string
     */
    public String convertToBase64(Serializable object) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream;
        try {
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(object);
            objectOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
    }

    /**
     * Used to convert a base64 string into an object
     *
     * @param string is the base64 string
     * @return an object
     */
    public Object convertBase64ToObject(String string) {
        byte[] data = Base64.getDecoder().decode(string);
        ObjectInputStream objectInputStream;
        Object object = null;
        try {
            objectInputStream = new ObjectInputStream(new ByteArrayInputStream(data));
            object = objectInputStream.readObject();
            objectInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return object;
    }

    /**
     * Deserialize a byte array into an ItemStack.
     *
     * @param data Data to deserialize.
     * @return Deserialized ItemStack.
     */
    public ItemStack deserializeItem(byte[] data) {
        ItemStack item = null;
        try (BukkitObjectInputStream stream = new BukkitObjectInputStream(new ByteArrayInputStream(data))) {
            item = (ItemStack) stream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return item;
    }

    /**
     * Serialize an ItemStack into a byte array.
     *
     * @param item Item to serialize.
     * @return Serialized data.
     */
    public byte[] serializeItem(ItemStack item) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream(); BukkitObjectOutputStream bukkitStream = new BukkitObjectOutputStream(stream)) {
            bukkitStream.writeObject(item);
            return stream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Used to create a player head
     *
     * @param name is the name of the player
     * @return the player skull
     */
    public ItemStack getPlayerHead(String name) {
        ItemStack stack = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        meta.setOwner(name);
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Used to send a discord message to a webhook link
     *
     * @param seller      The Seller of the auction item
     * @param buyer       The Buyer of the auction item
     * @param auctionItem The object of the auction item
     * @param saleType    The sale type, was it a bid won or an immediate purchase?
     * @param isNew       Is this the start of a new auction or the end of one?
     * @param isBid       Is this auction a bid enabled auction, or a single sale auction?
     */
    public void sendDiscordMessage(String webhook, OfflinePlayer seller, OfflinePlayer buyer, AuctionItem auctionItem, AuctionSaleType saleType, boolean isNew, boolean isBid) {
        DiscordWebhook hook = new DiscordWebhook(webhook);
        hook.setUsername(Settings.DISCORD_MSG_USERNAME.getString());
        hook.setAvatarUrl(Settings.DISCORD_MSG_PFP.getString());

        String[] possibleColours = Settings.DISCORD_MSG_DEFAULT_COLOUR.getString().split("-");
        Color colour = Settings.DISCORD_MSG_USE_RANDOM_COLOUR.getBoolean()
                ? Color.getHSBColor(ThreadLocalRandom.current().nextFloat() * 360F, ThreadLocalRandom.current().nextFloat() * 101F, ThreadLocalRandom.current().nextFloat() * 101F)
                : Color.getHSBColor(Float.parseFloat(possibleColours[0]) / 360, Float.parseFloat(possibleColours[1]) / 100, Float.parseFloat(possibleColours[2]) / 100);

        hook.addEmbed(new DiscordWebhook.EmbedObject()
                .setTitle(isNew ? Settings.DISCORD_MSG_START_TITLE.getString() : Settings.DISCORD_MSG_FINISH_TITLE.getString())
                .setColor(colour)
                .addField(Settings.DISCORD_MSG_FIELD_SELLER_NAME.getString(), Settings.DISCORD_MSG_FIELD_SELLER_VALUE.getString().replace("%seller%", seller.getName() != null ? seller.getName() : AuctionHouse.getInstance().getLocale().getMessage("discord.player_lost").getMessage()), Settings.DISCORD_MSG_FIELD_SELLER_INLINE.getBoolean())
                .addField(Settings.DISCORD_MSG_FIELD_BUYER_NAME.getString(), isNew ? AuctionHouse.getInstance().getLocale().getMessage("discord.no_buyer").getMessage() : Settings.DISCORD_MSG_FIELD_BUYER_VALUE.getString().replace("%buyer%", buyer.getName() != null ? buyer.getName() : AuctionHouse.getInstance().getLocale().getMessage("discord.player_lost").getMessage()), Settings.DISCORD_MSG_FIELD_BUYER_INLINE.getBoolean())
                .addField(Settings.DISCORD_MSG_FIELD_BUY_NOW_PRICE_NAME.getString(), Settings.DISCORD_MSG_FIELD_BUY_NOW_PRICE_VALUE.getString().replace("%buy_now_price%", this.getFriendlyNumber(auctionItem.getBasePrice())), Settings.DISCORD_MSG_FIELD_BUY_NOW_PRICE_INLINE.getBoolean())
                .addField(Settings.DISCORD_MSG_FIELD_FINAL_PRICE_NAME.getString(), isNew ? AuctionHouse.getInstance().getLocale().getMessage("discord.not_sold").getMessage() : Settings.DISCORD_MSG_FIELD_FINAL_PRICE_VALUE.getString().replace("%final_price%", this.getFriendlyNumber(isBid ? auctionItem.getCurrentPrice() : auctionItem.getBasePrice())), Settings.DISCORD_MSG_FIELD_FINAL_PRICE_INLINE.getBoolean())
                .addField(Settings.DISCORD_MSG_FIELD_IS_BID_NAME.getString(), Settings.DISCORD_MSG_FIELD_IS_BID_VALUE.getString().replace("%is_bid%", isBid ? AuctionHouse.getInstance().getLocale().getMessage("discord.is_bid_true").getMessage() : AuctionHouse.getInstance().getLocale().getMessage("discord.is_bid_false").getMessage()), Settings.DISCORD_MSG_FIELD_IS_BID_INLINE.getBoolean())
                .addField(Settings.DISCORD_MSG_FIELD_PURCHASE_TYPE_NAME.getString(), isNew ? AuctionHouse.getInstance().getLocale().getMessage("discord.not_bought").getMessage() : Settings.DISCORD_MSG_FIELD_PURCHASE_TYPE_VALUE.getString().replace("%purchase_type%", saleType == AuctionSaleType.USED_BIDDING_SYSTEM ? AuctionHouse.getInstance().getLocale().getMessage("discord.sale_bid_win").getMessage() : AuctionHouse.getInstance().getLocale().getMessage("discord.sale_immediate_buy").getMessage()), Settings.DISCORD_MSG_FIELD_PURCHASE_INLINE.getBoolean())
                .addField(Settings.DISCORD_MSG_FIELD_ITEM_NAME.getString(), Settings.DISCORD_MSG_FIELD_ITEM_VALUE.getString().replace("%item_name%", ChatColor.stripColor(getItemName(deserializeItem(auctionItem.getRawItem())))), Settings.DISCORD_MSG_FIELD_ITEM_INLINE.getBoolean())
                .addField(Settings.DISCORD_MSG_FIELD_ITEM_AMOUNT_NAME.getString(), Settings.DISCORD_MSG_FIELD_ITEM_AMOUNT_VALUE.getString().replace("%item_amount%", String.valueOf(this.deserializeItem(auctionItem.getRawItem()).getAmount())), Settings.DISCORD_MSG_FIELD_ITEM_AMOUNT_INLINE.getBoolean())
        );

        try {
            hook.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the name of an item stack
     *
     * @param stack is the item you want to get name from
     * @return the item name
     */
    public String getItemName(ItemStack stack) {
        Objects.requireNonNull(stack, "Item stack cannot be null when getting name");
        return stack.getItemMeta().hasDisplayName() ? stack.getItemMeta().getDisplayName() : TextUtils.formatText("&f" + WordUtils.capitalize(stack.getType().name().toLowerCase().replace("_", " ")));
    }

    /**
     * Used to get the lore from an item stack
     *
     * @param stack is the item being checked
     * @return the item lore if available
     */
    public List<String> getItemLore(ItemStack stack) {
        List<String> lore = new ArrayList<>();
        Objects.requireNonNull(stack, "Item stack cannot be null when getting lore");
        if (stack.hasItemMeta()) {
            if (stack.getItemMeta().hasLore() && stack.getItemMeta().getLore() != null) {
                lore.addAll(stack.getItemMeta().getLore());
            }
        }
        return lore;
    }

    /**
     * Used to get the names of all the enchantments on an item
     *
     * @param stack is the itemstack being checked
     * @return a list of all the enchantment names
     */
    public List<String> getItemEnchantments(ItemStack stack) {
        List<String> enchantments = new ArrayList<>();
        Objects.requireNonNull(stack, "Item Stack cannot be null when getting enchantments");
        if (!stack.getEnchantments().isEmpty()) {
            stack.getEnchantments().forEach((k, i) -> {
                enchantments.add(k.getName());
            });
        }
        return enchantments;
    }

    /**
     * Used to match patterns
     *
     * @param pattern  is the keyword being searched for
     * @param sentence is the sentence you're checking
     * @return whether the keyword is found
     */
    public boolean match(String pattern, String sentence) {
        Pattern patt = Pattern.compile(ChatColor.stripColor(pattern), Pattern.CASE_INSENSITIVE);
        Matcher matcher = patt.matcher(sentence);
        return matcher.find();
    }

    /**
     * @param pattern is the keyword that you're currently searching for
     * @param lines   is the lines being checked for the keyword
     * @return whether the keyword was found in any of the lines provided
     */
    public boolean match(String pattern, List<String> lines) {
        for (String line : lines) {
            if (match(pattern, line)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a sentence matches the format to convert it into seconds
     *
     * @param sentence is the string being checked
     * @return true if the string matches the time format
     */
    public boolean isValidTimeString(String sentence) {
        Pattern pattern = Pattern.compile("([0-9]){1,10}(s|m|h|d|y){1}", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sentence);
        return matcher.matches();
    }

    /**
     * Used to format numbers with decimals and commas
     *
     * @param number is the number you want to format
     * @return the formatted number string
     */
    public String formatNumber(double number) {
        String formatted = String.format("%,.2f", number);
        return Settings.USE_ALTERNATE_CURRENCY_FORMAT.getBoolean() ? replaceLast(formatted.replace(",", "."), ".", ",") : formatted;
    }

    /**
     * Used to replace the last portion of a string
     *
     * @param string      is the string being edited
     * @param substring   is the to replace word/phrase
     * @param replacement is the keyword(s) you're replacing the old substring with
     * @return the updated string
     */
    private String replaceLast(String string, String substring, String replacement) {
        int index = string.lastIndexOf(substring);
        if (index == -1) return string;
        return string.substring(0, index) + replacement + string.substring(index + substring.length());
    }

    /**
     * Used to get command flags (ex. -h, -f, -t, etc)
     *
     * @param args is the arguments passed when running a command
     * @return any command flags if any
     */
    public List<String> getCommandFlags(String... args) {
        List<String> flags = new ArrayList<>();
        for (String arg : args) {
            if (arg.startsWith("-") && arg.length() >= 2) {
                flags.add(arg.substring(0, 2));
            }
        }
        return flags;
    }

    /**
     * Get the total amount of an item in the player's inventory
     *
     * @param player is the player being checked
     * @param stack  is the item you want to find
     * @return the total count of the item(s)
     */
    public int getItemCountInPlayerInventory(Player player, ItemStack stack) {
        int total = 0;
        if (stack.getType() == XMaterial.PLAYER_HEAD.parseMaterial()) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null || item.getType() != XMaterial.PLAYER_HEAD.parseMaterial()) continue;
                if (NBTEditor.getTexture(item).equals(NBTEditor.getTexture(stack))) total += item.getAmount();
            }
        } else {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null || !item.isSimilar(stack)) continue;
                total += item.getAmount();
            }
        }
        return total;
    }

    /**
     * Used to get any items that are similar to the provided stack in a player's inventory
     *
     * @param player is the player being checked
     * @param stack  the item stack is being looked for
     * @return all the items that are similar to the stack
     */
    public List<ItemStack> getSimilarItemsFromInventory(Player player, ItemStack stack) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null) continue;
            if (stack.getType() == XMaterial.PLAYER_HEAD.parseMaterial() && item.getType() == XMaterial.PLAYER_HEAD.parseMaterial()) {
                if (!NBTEditor.getTexture(item).equals(NBTEditor.getTexture(stack))) continue;
            } else {
                if (!item.isSimilar(stack)) continue;
            }

            items.add(item);
        }

        return items;
    }

    /**
     * Removes a set amount of a specific item from the player inventory
     *
     * @param player is the player you want to remove the item from
     * @param stack  is the item that you want to remove
     * @param amount is the amount of items you want to remove.
     */
    public void removeSpecificItemQuantityFromPlayer(Player player, ItemStack stack, int amount) {
        int i = amount;
        for (int j = 0; j < player.getInventory().getSize(); j++) {
            ItemStack item = player.getInventory().getItem(j);
            if (item == null) continue;
            if (stack.getType() == XMaterial.PLAYER_HEAD.parseMaterial() && item.getType() == XMaterial.PLAYER_HEAD.parseMaterial()) {
                if (!NBTEditor.getTexture(item).equals(NBTEditor.getTexture(stack))) continue;
            } else {
                if (!item.isSimilar(stack)) continue;

            }

            if (i >= item.getAmount()) {
                player.getInventory().clear(j);
                i -= item.getAmount();
            } else if (i > 0) {
                item.setAmount(item.getAmount() - i);
                i = 0;
            } else {
                break;
            }
        }
    }

    /**
     * Used to create an item bundle
     *
     * @param baseItem is the base item of the bundle (original)
     * @param items    is the items that should be added to the bundle
     * @return an item stack with all the items saved in NBT tags
     */
    public ItemStack createBundledItem(ItemStack baseItem, ItemStack... items) {
        Objects.requireNonNull(items, "Cannot create a bundled item with no items");
        ItemStack item = ConfigurationItemHelper.createConfigurationItem(Settings.ITEM_BUNDLE_ITEM.getString(), Settings.ITEM_BUNDLE_NAME.getString(), Settings.ITEM_BUNDLE_LORE.getStringList(), new HashMap<String, Object>() {{
            put("%item_name%", getItemName(baseItem));
        }});

        int total = items.length;
        item = NBTEditor.set(item, total, "AuctionBundleItem");
        item = NBTEditor.set(item, UUID.randomUUID().toString(), "AuctionBundleItemUUID-" + UUID.randomUUID().toString());

        for (int i = 0; i < total; i++) {
            item = NBTEditor.set(item, serializeItem(items[i]), "AuctionBundleItem-" + i);
        }

        ItemUtils.addGlow(item);
        return item;
    }

    /**
     * Take a string like 5d and convert it into seconds
     * Valid suffixes:  m, d, w, mn, y
     *
     * @param time is the string time that will be converted
     * @return the total amount of seconds
     */
    public long getSecondsFromString(String time) {
        time = time.toLowerCase();
        char suffix = time.charAt(time.length() - 1);
        int amount = Character.getNumericValue(time.charAt(time.length() - 2));
        switch (suffix) {
            case 's':
                return amount;
            case 'm':
                return (long) amount * 60;
            case 'h':
                return (long) amount * 3600;
            case 'd':
                return (long) amount * 3600 * 24;
            case 'y':
                return (long) amount * 3600 * 24 * 365;
            default:
                return 0L;
        }
    }
}
