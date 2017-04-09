package me.tuke.sktuke.sections.gui;

import ch.njol.skript.Skript;
import ch.njol.skript.variables.Variables;
import me.tuke.sktuke.TuSKe;
import me.tuke.sktuke.util.NewRegister;
import me.tuke.sktuke.util.ReflectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import me.tuke.sktuke.util.EffectSection;

import java.util.*;

/**
 * @author Tuke_Nuke on 01/04/2017
 */
public class EffMakeGUI extends EffectSection {
	static {
		NewRegister.newEffect(EffMakeGUI.class,
				"make next gui [slot] (with|to) %itemstack%",
				"make gui [slot] %string/number% (with|to) %itemstack%"
				//"make next gui [slot] (with|to) %itemstack% with (var[iable]|value)[s] %objects%",
				//"make gui [slot] %string/number% (with|to) %itemstack% with (var[iable]|value)[s] %objects%"
				);
	}
	public static WeakHashMap<Event, Object> map = ReflectionUtils.getField(Variables.class, null, "localVariables");
	public static EffMakeGUI lastInstance = null;

	private EffCreateGUI effGui;
	private Expression<?> slot; //Can be number or a string
	//private Expression<?> where; //Can be the player or a gui inventory.
	private Expression<ItemStack> item;
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] arg, int arg1, Kleenean arg2, ParseResult arg3) {
		if (checkIfCondition()) {
			return false;
		}
		if (EffCreateGUI.lastInstance == null) {
			Skript.error("You can't use 'make gui effect' out side of 'create gui effect'");
			return false;
		}
		if (arg1 % 2 == 0) {
			item = (Expression<ItemStack>) arg[0];
		} else {
			slot = arg[0].getConvertedExpression(Object.class);
			item = (Expression<ItemStack>) arg[1];
		}
		effGui = EffCreateGUI.lastInstance;
		if (hasSection()) {
			EffMakeGUI last = lastInstance;
			lastInstance = this;
			loadSection("gui effect", InventoryClickEvent.class);
			lastInstance = last;
			//loadSection();
		}
		return true;
	}

	@Override
	public void execute(Event e) {
		ItemStack item = this.item.getSingle(e);
		//if (item == null)
		//	item = new ItemStack(Material.AIR);
		Object[] slot = this.slot != null ? this.slot.getArray(e) : new Object[]{effGui.gui.nextSlot()};
		for (Object s : slot) {
			if (hasSection()) {
				Object variables = copyVariables(e);
				effGui.gui.setItem(s, item, event -> {
					pasteVariables(event, variables);
					runSection(event);
				});
			} else
				effGui.gui.setItem(s, item);
		}
	}
	
	@Override
	public String toString(Event arg0, boolean arg1) {
		return "make " + (slot != null ? "a gui slot "+ slot.toString(arg0, arg1) : "next gui slot") + " of gui with " + item.toString(arg0, arg1);
	}
	//
	// Some hacking methods to copy variables from one event, and paste
	// to another. It allows to run the section using the same variables
	// when was making the gui
	//
	@SuppressWarnings("unchecked")
	public Object copyVariables(Event from){
		if (from != null && map.containsKey(from)) {
			Object variablesMap = map.get(from);
			if (variablesMap == null)
				return null;
			Object newVariablesMap = ReflectionUtils.newInstance(variablesMap.getClass());
			//TuSKe.debug(newVariablesMap, variablesMap);
			if (newVariablesMap == null)
				return null;
			HashMap<String, Object> single = ReflectionUtils.getField(newVariablesMap.getClass(), newVariablesMap, "hashMap");
			TreeMap<String, Object> list = ReflectionUtils.getField(newVariablesMap.getClass(), newVariablesMap, "treeMap");
			single.putAll(ReflectionUtils.getField(variablesMap.getClass(), variablesMap, "hashMap"));
			list.putAll(ReflectionUtils.getField(variablesMap.getClass(), variablesMap, "treeMap"));
			return newVariablesMap;
		}
		return null;
	}
	private Map<String, Object> getMap(Object variablesMap, String fieldName) {
		return ReflectionUtils.getField(variablesMap.getClass(), variablesMap, fieldName);
	}
	public void pasteVariables(Event to, Object variables){
		if (to != null)
			map.put(to, variables);
	}
}