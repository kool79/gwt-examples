package org.gonevertical.core.client.input.richtext;

import org.gonevertical.core.client.html.HtmlSanitizerUtils;
import org.gonevertical.core.client.onpaste.OnPasteUtils;
import org.gonevertical.core.client.style.ComputedStyle;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.DomEvent.Type;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.EventPreview;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.RootPanel;

public class WiseRichTextArea extends RichTextArea {

  /**
   * clone these style properties for size calculation
   */
  private String[] styles = { 
      "color",
      "direction", 

      "fontFace", // @fontFace?
      "fontFamily", 
      "fontSize", 
      "fontSizeAdjust",
      "fontStretch",
      "fontStyle",
      "fontVariant",
      "fontWeight",

      "letterSpacing", 
      "lineHeight", 

      "padding",
      "paddingBottom",
      "paddingLeft",
      "paddingRight",
      "paddingTop",

      "textAlign",
      "textDecoration",
      "textIndent",
      "textJustify",
      "textOutline",
      "textShawdow",
      "textTransform",

      "wordSpacing"

      //"whiteSpacing" ?
      //"punctuationTrim" ?
  };

  /**
   * original TextArea size set
   */
  private boolean orginalSet;

  /**
   * original TextArea width
   */
  private int originalWidth;

  /**
   * orginal TextArea height
   */
  private int originalHeight;

  /**
   * allow some room for future typing
   */
  private int headroomWidthPadding = 7;

  /**
   * allow for some room below the cursor
   */
  private int headroomHeightPadding = 14;

  /**
   * someone for the htmlForSizeTesting to hide in
   */
  private AbsolutePanel hiddenPanel;

  /**
   * somewhere to calculate the size
   */
  private HTML htmlForSizeTesting;

  /**
   * track toucheds to element
   */
  private boolean isTouched;

  /**
   * calculated width with headroomWidth padding
   */
  private int width;

  /**
   * calculated height with headroomHeight padding
   */
  private int height;

  private boolean delayedClone;

  private boolean clonedOnce;

  private boolean hideBorderUntilHover;

  private boolean grow;

  private String defaultText;

  /**
   * constructor
   */
  public WiseRichTextArea() {
    super();
    setup(false, true);
  }

  /**
   * constructor
   * @param hideBorderUntilHover - hover over box to see border
   * @param grow - auto grow to size of text
   */
  public WiseRichTextArea(boolean hideBorderUntilHover, boolean grow) {
    super();
    setup(hideBorderUntilHover, grow);
  }

  private void setup(boolean hideBorderUntilHover, boolean grow) {
    this.hideBorderUntilHover = hideBorderUntilHover;
    this.grow = grow;

    addStyleName("gv-core-WiseRichTextArea");

    setUpEditHover();  

    setupHandlers();


    // deal with messy pasting
    /**
     * TODO - No Worky - iframe? I see the ook events in the RichTextAreaImpl and how come I can't hook them in here?
     */
    sinkEvents(Event.ONPASTE); // TODO ? no worky
    DOM.sinkEvents(getElement(), Event.ONPASTE); //TODO ? no worky
    
    //sinkEvents(Event.ONKEYUP);

    /**
     * TODO this won't work either, b/c it won't capture once focued on richtextarea  
     */
    Event.addNativePreviewHandler(new NativePreviewHandler() {
      public void onPreviewNativeEvent(NativePreviewEvent event) {
        NativeEvent ne = event.getNativeEvent();
        if (event.getTypeInt() == Event.ONPASTE) {
          System.out.println("Pasting?????");
          Window.alert("works");
        }
        System.out.println("event.toDebutString()=" + event.toDebugString() + " ne.getType=" + ne.getType() + " ne.toString=" + ne.toString() + " charCode=" + ne.getCharCode() + " key=" + (char)ne.getCharCode());
      }
    });

  }
  
  
  
  /**
   * TODO corresponding to sink - no events coming in yet
   */
  @Override 
  public void onBrowserEvent(Event event) { 
    super.onBrowserEvent(event); 
    switch (event.getTypeInt()) { 
    case Event.ONPASTE: 
      System.out.println("PASTE DETECTED"); 
      Window.alert("Works 2");
      break; 
    } 
  } 

  
  
  

  private void setupHandlers() {

    addMouseOverHandler(new MouseOverHandler() {
      public void onMouseOver(MouseOverEvent event) {
        if (hideBorderUntilHover == true) {
          setEdit(true);
        }
      }
    });

    addMouseOutHandler(new MouseOutHandler() {
      public void onMouseOut(MouseOutEvent event) {
        if (hideBorderUntilHover == true) {
          setEdit(false);  
        }
      }
    });

    addTouchStartHandler(new TouchStartHandler() {
      public void onTouchStart(TouchStartEvent event) {
        if (hideBorderUntilHover == true) {
          setTouched();  
        }
      }
    });

    addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        if (defaultText != null) {
          drawDefaultText();
        }
        if (grow == true) {
          setNewSize(false);
        }
      }
    });

    addKeyPressHandler(new KeyPressHandler() {
      public void onKeyPress(KeyPressEvent event) {
        NativeEvent ne = event.getNativeEvent();
        System.out.println("KeyPressHandler=" + ne.getType() + " key=" + (char)ne.getCharCode());
        if (grow == true) {
          if (event.getCharCode() == KeyCodes.KEY_ENTER) {
            setNewSize(true);
          }
        }
      }
    });
    
    addKeyDownHandler(new KeyDownHandler() {
      public void onKeyDown(KeyDownEvent event) {
        NativeEvent ne = event.getNativeEvent();
        //System.out.println("KeyDownHandler=" + ne.getType() + " key=" + (char)ne.getCharCode());
      }
    });

    /* TODO ????
    addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        setDefaultTextIntoTextBox();
      }
    });
     */

    addFocusHandler(new FocusHandler() {
      public void onFocus(FocusEvent event) {
        setZeroText();
      }
    });

    addBlurHandler(new BlurHandler() {
      public void onBlur(BlurEvent event) {
        setDefaultTextIntoTextBox();
      }
    });

  }

  public void setDefaultText(String defaultText) {
    this.defaultText = defaultText;
    addStyleName("gv-core-WiseRichTextArea-default");
    setHTML(defaultText);
  }

  private void drawDefaultText() {
    if (defaultText == null) {
      return;
    }
    if (getText().trim().equals(defaultText) == true) {
      addStyleName("gv-core-WiseRichTextArea-default");
    } else {
      removeStyleName("gv-core-WiseRichTextArea-default");
    }
  }


  /**
   * when focusing, and the default text is set, set it to zero text
   */
  private void setZeroText() {
    if (defaultText == null) {
      return;
    }
    if (defaultText.equals(getText().trim()) == true) {
      setHTML("");
    }
  }

  private void setDefaultTextIntoTextBox() {
    if (getText().trim().length() != 0) {
      return;
    }
    addStyleName("gv-core-WiseTextBox-default");
    setHTML(defaultText); 
  }

  /**
   * hide border until hovering over the input
   */
  private void setUpEditHover() {
    if (hideBorderUntilHover == true) {
      setEdit(false);
    }
  }

  public void setEdit(boolean edit) {
    if (edit == true) {
      setEdit();

    } else {
      setView();
    }
  }

  private void setTouched() {
    if (isTouched == false) {
      isTouched = true;
      setEdit(false);
    } else {
      isTouched = false;
      setEdit(true);
    }
  }

  private void setEdit() {
    removeStyleName("gv-core-WiseRichTextArea-noborder");
  }

  private void setView() {
    addStyleName("gv-core-WiseRichTextArea-noborder");
  }

  private void setNewSize(boolean forceNewLine) {

    // only done once
    setOriginalSize();

    // onlydone once - sets up a location to calculate size
    setHiddenPanel();

    // only done once
    cloneStyleOnce();

    // do everytime - check for textbox getting bigger
    //setMovingWidth();

    // everytime - calculate the size width and height of text
    setCurrentTextSize(forceNewLine);


    /* only use height, but could do both
    if (width > originalWidth) {
      setWidth(width + "px");
    }
     */

    if (height > originalHeight) {
      super.setHeight(height + "px");
    }
  }

  /**
   * for changing textbox width
   */
  private void setMovingWidth() {
    if (htmlForSizeTesting == null) {
      return;
    }
    //TODO what the heck is with the offset width differring. Maybe i'm tired. 
    int left = 0;
    try {
      left = Integer.parseInt(ComputedStyle.getStyleProperty(this.getElement(), "width").replaceAll("[^0-9]", ""));
    } catch (NumberFormatException e) {
      e.printStackTrace();
      return;
    }
    int right = htmlForSizeTesting.getOffsetWidth();
    if (left != right) {
      setTextContainerWidth(left + "");
    }
  }

  private void cloneStyleOnce() {
    if (clonedOnce == false) {
      clonedOnce = true;
      cloneStyle();
    }
  }

  /**
   * resize input 
   * @param forceNewLine - on enter, think ahead and set size, so no scroll bars appear
   */
  private void setCurrentTextSize(boolean forceNewLine) {

    String s = getHTML();

    if (forceNewLine == true) { // thing forward on height spacing
      s += "<br>";
    }

    System.out.println("s=" + s);

    SafeHtml sh = HtmlSanitizerUtils.sanitizeHtml(s); // I'm allowing more through the door here
    htmlForSizeTesting.setHTML(sh);

    width = htmlForSizeTesting.getOffsetWidth();
    width += headroomWidthPadding;

    height = htmlForSizeTesting.getOffsetHeight();
    height += headroomHeightPadding; 

    //System.out.println("setCurrentTextSize(): width=" + (width-headroomWidthPadding) + " height=" + (height-headroomHeightPadding));
  }

  private void setOriginalSize() {    
    if (orginalSet == false) {
      originalWidth = getOffsetWidth();
      originalHeight = getOffsetHeight();
      //System.out.println("setOriginalSize(): originalWidth=" + originalWidth + " originalHeight=" + originalHeight);
      orginalSet = true;
    }
  }

  private void cloneStyle() {
    if (htmlForSizeTesting == null) {
      return;
    }

    Element eleft = getElement();
    Element eright = htmlForSizeTesting.getElement();

    if (styles == null) { // skip the initial init
      return;
    }
    for (int i=0; i < styles.length; i++) {
      String prop = ComputedStyle.getStyleProperty(eleft, styles[i]);
      eright.getStyle().setProperty(styles[i], prop);

      // debug
      //System.out.println("TextArea cloneStyle() style=" + styles[i] + " prop=" + prop);
    }

    //eright.getStyle().setProperty("wordWrap", "break-word");
    // css3 long word breaking like it will break aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa....
    eright.getStyle().setProperty("wordBreak", "break-all");
  }

  /**
   * delay the clone style, b/c computed style seems to take a moment longer than.. not sure why
   */
  private void cloneStyleTimed() {
    if (delayedClone == true) {
      return;
    }
    delayedClone = true;
    Timer t = new Timer() {
      public void run() {
        cloneStyle();
        setNewSize(false);
      }
    };
    t.schedule(600);
  }

  /**
   * TODO move this to a shared hidden div.
   */
  private void setHiddenPanel() {
    if (htmlForSizeTesting != null) {
      return;
    }

    // create a panel I can hide
    hiddenPanel = new AbsolutePanel();
    RootPanel.get().add(hiddenPanel);

    // create a spot to measure html - note text area you would want to wrap
    htmlForSizeTesting = new HTML("", true); 
    //hiddenPanel.add(htmlForSizeTesting, -1000, -1000); // hide it from view
    hiddenPanel.add(htmlForSizeTesting);
    htmlForSizeTesting.setStyleName("gv-core-WiseRichTextArea-break"); // css3 long word breaking

    // setup width constraint
    setTextContainerWidth(Integer.toString(getOffsetWidth()));

    //debug
    hiddenPanel.addStyleName("test1");
    htmlForSizeTesting.addStyleName("test2");
  }

  private void setTextContainerWidth(String width) {
    if (htmlForSizeTesting == null) {
      return;
    }
    htmlForSizeTesting.setWidth(width + "px");
    //System.out.println("setTextContainerWidth=" + width);
  }

  @Override
  public void setHTML(String html) {
    if (defaultText != null && html.equals(defaultText) == false) {
      removeStyleName("gv-core-WiseTextBox-default"); // TODO setup a method for this
    }
    //TODO SafeHtml sh = HtmlSanitizerUtils.sanitizeHtml(html);
    super.setHTML(html);
    setNewSize(false);

    // delay for attatching or whatever. workaround here
    cloneStyleTimed();
  };

  @Override
  public void setStyleDependentName(String styleSuffix, boolean add) {
    super.setStyleDependentName(styleSuffix, add);
    cloneStyle();
  };

  @Override
  public void setStyleName(String style) {
    super.setStyleName(style);
    cloneStyle();
  }

  @Override
  public void setStylePrimaryName(String style) {
    super.setStylePrimaryName(style);
    cloneStyle();
  }

  @Override
  public void setWidth(String width) {
    super.setWidth(width);
    width = width.replaceAll("[^0-9]", "");
    try {
      originalWidth = Integer.parseInt(width);
    } catch (NumberFormatException e) {
    }

    setTextContainerWidth(width);
  }

  @Override
  public void setHeight(String height) {
    super.setHeight(height);
    height = height.replaceAll("[^0-9]", "");
    try {
      originalHeight = Integer.parseInt(height);
    } catch (NumberFormatException e) {
    }
  }

  @Override
  public void setSize(String width, String height) {
    setWidth(width);
    setHeight(height);
  }






}