package betterquesting.client;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import betterquesting.client.buttons.GuiButtonJson;
import betterquesting.client.buttons.GuiButtonQuesting;
import betterquesting.utils.JsonIO;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class GuiJsonArray extends GuiQuesting
{
	int scrollPos = 0;
	JsonArray settings;
	boolean allowEdit = true;
	
	/**
	 * List of GuiTextFields and GuiButtons
	 */
	ArrayList<JsonControlSet> editables = new ArrayList<JsonControlSet>();
	
	public GuiJsonArray(GuiScreen parent, JsonArray settings)
	{
		super(parent, "Editor - JSON Array");
		this.settings = settings;
	}
	
	/**
	 * Set whether or not a user can add/remove entries from this JsonElement
	 * @param state
	 * @return
	 */
	public GuiJsonArray SetEditMode(boolean state)
	{
		this.allowEdit = state;
		return this;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void initGui()
	{
		super.initGui();
		
		editables = new ArrayList<JsonControlSet>();;
		
		((GuiButton)this.buttonList.get(0)).xPosition = this.width/2 - 100;
		((GuiButton)this.buttonList.get(0)).width = 100;
		this.buttonList.add(new GuiButtonQuesting(1, this.width/2, this.guiTop + this.sizeY - 16, 100, 20, "Add"));
		this.buttonList.add(new GuiButtonQuesting(2, this.width/2, this.guiTop + (this.sizeY - 84)/20 * 20 + 30, 20, 20, "<"));
		this.buttonList.add(new GuiButtonQuesting(3, this.guiLeft + this.sizeX - 36, this.guiTop + (this.sizeY - 84)/20 * 20 + 30, 20, 20, ">"));
		
        for(int i = 0; i < settings.size(); i++)
		{
			JsonElement entry = settings.get(i);
			if(entry.isJsonPrimitive())
			{
				JsonPrimitive jPrim = entry.getAsJsonPrimitive();
				GuiTextField txtBox;
				if(jPrim.isNumber())
				{
					txtBox = new GuiNumberField(this.fontRendererObj, 32, -9999, 128, 16);
					txtBox.setText("" + jPrim.getAsNumber());
				} else if(jPrim.isBoolean())
				{
					GuiButtonJson button = new GuiButtonJson(buttonList.size(), -9999, -9999, 128, 20, jPrim);
					editables.add(new JsonControlSet(this.buttonList, button, true, true));
					this.buttonList.add(button);
					continue;
				} else
				{
					txtBox = new GuiTextField(this.fontRendererObj, 32, -9999, 128, 16);
					txtBox.setMaxStringLength(Integer.MAX_VALUE);
					txtBox.setText(jPrim.getAsString());
				}
				
				editables.add(new JsonControlSet(this.buttonList, txtBox, true, true));
			} else
			{
				GuiButtonJson button = new GuiButtonJson(buttonList.size(), -9999, -9999, 128, 20, entry);
				editables.add(new JsonControlSet(this.buttonList, button, true, true));
				this.buttonList.add(button);
			}
		}
	}
	
	@Override
	public void onGuiClosed()
	{
		// >> Send new settings to the server here <<
	}
	
	@Override
	public void actionPerformed(GuiButton button)
	{
		if(button.id == 0)
		{
			this.mc.displayGuiScreen(parent);
		} else if(button.id == 1)
		{
			this.mc.displayGuiScreen(new GuiJsonAdd(this, this.settings, this.settings.size()));
		} else if(button.id == 2)
		{
			if(scrollPos > 0)
			{
				scrollPos -= 1;
			} else
			{
				scrollPos = 0;
			}
		} else if(button.id == 3)
		{
			int maxShow = (this.sizeY - 84)/20;
			if((scrollPos + 1) * maxShow < editables.size())
			{
				scrollPos += 1;
			}
		} else
		{
			for(int key = 0; key < editables.size(); key++)
			{
				JsonControlSet controls = editables.get(key);
				
				if(controls == null)
				{
					continue;
				}
				
				if(button == controls.addButton)
				{
					this.mc.displayGuiScreen(new GuiJsonAdd(this, this.settings, key));
					break;
				} else if(button == controls.removeButton)
				{
					ArrayList<JsonElement> list = JsonIO.GetUnderlyingArray(this.settings);
					list.remove(key);
					this.buttonList.remove(controls.addButton);
					this.buttonList.remove(controls.removeButton);
					this.buttonList.remove(controls.jsonDisplay);
					editables.remove(key);
					break;
				} else if(button == controls.jsonDisplay && button instanceof GuiButtonJson)
				{
					GuiButtonJson jsonButton = (GuiButtonJson)button;
					JsonElement element = jsonButton.json;
					
					if(jsonButton.isItemStack() || jsonButton.isEntity())
					{
						this.mc.displayGuiScreen(new GuiJsonTypeMenu(this, element.getAsJsonObject()));
					} else if(element.isJsonObject())
					{
						this.mc.displayGuiScreen(new GuiJsonObject(this, element.getAsJsonObject()).SetEditMode(this.allowEdit));
					} else if(element.isJsonArray())
					{
						this.mc.displayGuiScreen(new GuiJsonArray(this, element.getAsJsonArray()).SetEditMode(this.allowEdit));
					} else if(element.isJsonPrimitive())
					{
						if(element.getAsJsonPrimitive().isBoolean())
						{
							JsonPrimitive jBool = new JsonPrimitive(!element.getAsBoolean());
							
							// Make shift 'put' method for out dated GSON library
							
							ArrayList<JsonElement> list = JsonIO.GetUnderlyingArray(settings);
							
							if(list != null)
							{
								list.set(key, jBool);
							} else
							{
								break;
							}
							
							
							jsonButton.displayString = "" + jBool.getAsBoolean();
							jsonButton.json = jBool;
						}
					}
					break;
				}
			}
		}
	}
	
	@Override
	public void drawScreen(int mx, int my, float partialTick)
	{
		super.drawScreen(mx, my, partialTick);
		
		int maxRows = (this.sizeY - 84)/20;
		
		for(int i = 0; i < editables.size(); i++)
		{
			JsonControlSet controls = editables.get(i);
			
			int n = i - (scrollPos * maxRows);
			int posX = this.guiLeft + (sizeX/2);
			int posY = -9999;
			
			if(n >= 0 && n < maxRows)
			{
				posY = this.guiTop + 30 + (n * 20);
				this.fontRendererObj.drawString("#" + i, posX - this.fontRendererObj.getStringWidth("#" + i) - 8, posY + 4, Color.BLACK.getRGB(), false);
				
				if(controls != null)
				{
					controls.drawControls(this, posX, posY, sizeX/2 - 16, 20, mx, my, partialTick);
				}
			} else
			{
				controls.Disable();
			}
		}
	}
	
	@Override
	public void mouseClicked(int x, int y, int type)
	{
		super.mouseClicked(x, y, type);
		
		for(JsonControlSet control : editables)
		{
			if(control == null)
			{
				continue;
			}
			
			control.mouseClick(this, x, y, type);
		}
	}
	
    /**
     * Fired when a key is typed. This is the equivalent of KeyListener.keyTyped(KeyEvent e).
     */
	@Override
    protected void keyTyped(char character, int num)
    {
		super.keyTyped(character, num);
		
		ArrayList<JsonElement> list = JsonIO.GetUnderlyingArray(settings);
		
		if(list == null)
		{
			return;
		}
		
		for(int i = 0; i < editables.size(); i++)
		{
			JsonControlSet controls = editables.get(i);
			
			if(controls.jsonDisplay instanceof GuiTextField)
			{
				GuiTextField textField = (GuiTextField)controls.jsonDisplay;
				textField.textboxKeyTyped(character, num);
				
				if(list.get(i).getAsJsonPrimitive().isNumber())
				{
					try
					{
						list.set(i, new JsonPrimitive(NumberFormat.getInstance().parse(textField.getText())));
					} catch(Exception e)
					{
						list.set(i, new JsonPrimitive(textField.getText()));
					}
				} else
				{
					list.set(i, new JsonPrimitive(textField.getText()));
				}
			}
		}
    }
}
