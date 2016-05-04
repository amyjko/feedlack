package edu.uw.feedlack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class JavaScriptDictionary {
	
	public static final Map<String, JavaScriptType> PRIMITIVES = new HashMap<String,JavaScriptType>();
	
	public static boolean isInputEventNameRequiringOutput(String name) { return INPUT_EVENTS_REQUIRING_OUTPUT.contains(name); }
	
	public static boolean isJQueryEventName(String name) { return JQUERY_EVENT_NAMES.contains(name); }
	
	public static final JavaScriptType DOCUMENT = new JavaScriptType("document");
	public static final JavaScriptType WINDOW = new JavaScriptType("window");
	public static final JavaScriptType ELEMENT = new JavaScriptType("Element");
	public static final JavaScriptType GLOBAL = new JavaScriptType("Global");
	public static final JavaScriptType ARRAY = new JavaScriptType("Array");
	public static final JavaScriptType BOOL = new JavaScriptType("Bool");
	public static final JavaScriptType NONE = new JavaScriptType("None");
	public static final JavaScriptType FUNCTION = new JavaScriptType("Function");
	public static final JavaScriptType NUMBER = new JavaScriptType("Number");
	public static final JavaScriptType STYLE = new JavaScriptType("Style");
	public static final JavaScriptType JQUERY = new JavaScriptType("jQuery");
	public static final JavaScriptType STRING = new JavaScriptType("String");
	public static final JavaScriptType LOCATION = new JavaScriptType("location");
	public static final JavaScriptType CONTEXT2D = new JavaScriptType("context2D");

	static {
		
		STRING.addFunction("indexOf", NUMBER);
		STRING.addFunction("lastIndexOf", NUMBER);
		STRING.addFunction("split", NUMBER);
		STRING.addFunction("charAt", NUMBER);
		
		DOCUMENT.addFunction("open", NONE, Likelihood.YES, "opens a new window");
		DOCUMENT.addFunction("close", NONE, Likelihood.YES, "closes the window");
		DOCUMENT.addFunction("write", NONE, Likelihood.YES, "writes to the DOM");
		DOCUMENT.addFunction("writeln", NONE, Likelihood.YES, "writes to the DOM");
		
		DOCUMENT.addFunction("createElement", ELEMENT);
		DOCUMENT.addFunction("createDocumentFragment", ELEMENT);
		DOCUMENT.addFunction("createTextNode", NONE);
		DOCUMENT.addFunction("createComment", NONE);
		DOCUMENT.addFunction("createCDATASection", NONE);
		DOCUMENT.addFunction("createProcessingInstruction", NONE);
		DOCUMENT.addFunction("createAttribute", NONE);
		DOCUMENT.addFunction("createEntityReference", NONE);
		DOCUMENT.addFunction("getElementsByTagName", ARRAY);
		DOCUMENT.addFunction("getElementsByClassName", ARRAY);
		DOCUMENT.addFunction("getElementsByName", ARRAY);
		DOCUMENT.addFunction("importNode", NONE);
		DOCUMENT.addFunction("createElementNS", NONE);
		DOCUMENT.addFunction("createAttributeNS", NONE);
		DOCUMENT.addFunction("getElementsByTagNameNS", NONE);
		DOCUMENT.addFunction("getElementById", ELEMENT);
		DOCUMENT.addFunction("adoptNode", NONE);
		DOCUMENT.addFunction("normalizeDocument", NONE);
		DOCUMENT.addFunction("renameNode", NONE);

		DOCUMENT.addProperty("location", STRING, Likelihood.YES, "opens a new window");
		DOCUMENT.addProperty("body", ELEMENT);
		
		WINDOW.addFunction("prompt", NONE, Likelihood.YES, "opens a popup");
		WINDOW.addFunction("focus", NONE, Likelihood.YES, "may already be in focus");
		WINDOW.addFunction("close", NONE, Likelihood.YES, "closes window");
		WINDOW.addFunction("alert", NONE, Likelihood.YES, "shows popup");
		WINDOW.addFunction("confirm", NONE, Likelihood.YES, "shows popup");
		WINDOW.addFunction("blur", NONE, Likelihood.YES, "may already be out of focus");
		WINDOW.addFunction("moveBy", NONE, Likelihood.YES, "moves window");
		WINDOW.addFunction("moveTo", NONE, Likelihood.YES, "moves window");
		WINDOW.addFunction("open", NONE, Likelihood.YES, "opens window");
		WINDOW.addFunction("resizeBy", NONE, Likelihood.YES, "resizes window");
		WINDOW.addFunction("resizeTo", NONE, Likelihood.YES, "resizes window");
		WINDOW.addFunction("scrollBy", NONE, Likelihood.YES, "scrolls window");
		WINDOW.addFunction("scrollTo", NONE, Likelihood.YES, "scrolls window");
		WINDOW.addFunction("print", NONE, Likelihood.YES, "shows print dialog");

		WINDOW.addFunction("clearInterval", NONE);
		WINDOW.addFunction("clearTimeout", NONE);
		WINDOW.addFunction("confirm", NONE);
		WINDOW.addFunction("createPopup", NONE);
		WINDOW.addFunction("print", NONE);
		WINDOW.addFunction("setInterval", NONE);
		WINDOW.addFunction("setTimeout", NONE);

		WINDOW.addProperty("location", LOCATION, Likelihood.YES, "opens window");

		WINDOW.addProperty("closed", BOOL);
		WINDOW.addProperty("defaultStatus", NONE);
		WINDOW.addProperty("document", DOCUMENT);
		WINDOW.addProperty("frames", NONE);
		WINDOW.addProperty("history", NONE);
		WINDOW.addProperty("innerHeight", NONE);
		WINDOW.addProperty("innerWidth", NONE);
		WINDOW.addProperty("length", NONE);
		WINDOW.addProperty("name", NONE);
		WINDOW.addProperty("opener", NONE);
		WINDOW.addProperty("outerHeight", NONE);
		WINDOW.addProperty("outerWidth", NONE);
		WINDOW.addProperty("pageXOffset", NONE);
		WINDOW.addProperty("pageYOffset", NONE);
		WINDOW.addProperty("parent", NONE);
		WINDOW.addProperty("screenLeft", NONE);
		WINDOW.addProperty("screenTop", NONE);
		WINDOW.addProperty("screen", NONE);
		WINDOW.addProperty("screenX", NONE);
		WINDOW.addProperty("screenY", NONE);
		WINDOW.addProperty("self", NONE);
		WINDOW.addProperty("status", NONE);
		WINDOW.addProperty("top", NONE);
		
		WINDOW.addProperty("onblur", NONE);
		WINDOW.addProperty("onchange", NONE);
		WINDOW.addProperty("onclick", NONE);
		WINDOW.addProperty("ondblclick", NONE);
		WINDOW.addProperty("onfocus", NONE);
		WINDOW.addProperty("onkeydown", NONE);
		WINDOW.addProperty("onkeyup", NONE);
		WINDOW.addProperty("onload", NONE);
		WINDOW.addProperty("onmousedown", NONE);
		WINDOW.addProperty("onmousemove", NONE);
		WINDOW.addProperty("onmouseout", NONE);
		WINDOW.addProperty("onmouseover", NONE);
		WINDOW.addProperty("onmouseup", NONE);
		WINDOW.addProperty("onreset", NONE);
		WINDOW.addProperty("onselect", NONE);
		WINDOW.addProperty("onsubmit", NONE);
		WINDOW.addProperty("onreload", NONE);

		ELEMENT.addFunction("setAttribute", NONE, Likelihood.YES, "attribute may not affect output");
		ELEMENT.addFunction("removeChild", NONE, Likelihood.YES, "child may not be found");

		ELEMENT.addFunction("insertBefore", NONE, Likelihood.YES, "modifies DOM");
		ELEMENT.addFunction("replaceChild", NONE, Likelihood.YES, "modifies DOM");
		ELEMENT.addFunction("appendChild", NONE, Likelihood.YES, "modifies DOM");
		ELEMENT.addFunction("removeAttribute", NONE, Likelihood.YES, "attribute may not affect output");
		ELEMENT.addFunction("submit", NONE, Likelihood.YES, "submits form");
		ELEMENT.addFunction("reset", NONE, Likelihood.YES, "resets form");
		ELEMENT.addFunction("getContext", CONTEXT2D, Likelihood.NO, "");
		
		ELEMENT.addFunction("getElementsByTagName", ELEMENT);
		ELEMENT.addFunction("item", ELEMENT);

		// Non-output-affecting element properties
		ELEMENT.addProperty("style", STYLE);

		// Output-affecting element properties
		ELEMENT.addProperty("className", STRING, Likelihood.YES, "may already have class assigned");
		ELEMENT.addProperty("id", STRING, Likelihood.MAYBE, "may not affect CSS");
		ELEMENT.addProperty("scrollTop", NUMBER, Likelihood.YES, "scrolls element");
		ELEMENT.addProperty("scrollLeft", NUMBER, Likelihood.YES, "scrolls element");
		ELEMENT.addProperty("textContent", STRING, Likelihood.YES, "modifies DOM");
		ELEMENT.addProperty("innerHTML", STRING, Likelihood.YES, "modifies DOM");
		ELEMENT.addProperty("disabled", BOOL, Likelihood.YES, "enables or disables element");
		ELEMENT.addProperty("value", STRING, Likelihood.YES, "affects value of form control");
		ELEMENT.addProperty("checked", STRING, Likelihood.YES, "affects status of checkbox");
		ELEMENT.addProperty("parentNode", ELEMENT, Likelihood.YES, "affects status of checkbox");
		ELEMENT.addProperty("src", ELEMENT, Likelihood.YES, "affects image shown");
		ELEMENT.addProperty("href", STRING);
		
		GLOBAL.addFunction("$", JQUERY);
		GLOBAL.addFunction("alert", NONE, Likelihood.YES, "shows popup");
		GLOBAL.addFunction("confirm", BOOL, Likelihood.YES, "shows popup");
		GLOBAL.addFunction("prompt", NONE, Likelihood.YES, "shows popup");
		GLOBAL.addFunction("open", NONE, Likelihood.YES, "opens window");

		GLOBAL.addProperty("document", DOCUMENT);
		GLOBAL.addProperty("window", WINDOW);
		GLOBAL.addProperty("location", LOCATION);
		
		GLOBAL.addFunction("decodeURI", NONE);
		GLOBAL.addFunction("decodeURIComponent", NONE);
		GLOBAL.addFunction("encodeURI", NONE);
		GLOBAL.addFunction("encodeURIComponent", NONE);
		GLOBAL.addFunction("eval", NONE);
		GLOBAL.addFunction("isFinite", NONE);
		GLOBAL.addFunction("isNaN", NONE);
		GLOBAL.addFunction("parseFloat", NONE);
		GLOBAL.addFunction("parseInt", NONE);
		GLOBAL.addFunction("escape", NONE);
		GLOBAL.addFunction("unescape", NONE);
		GLOBAL.addFunction("addEventListener", NONE);
		GLOBAL.addFunction("removeEventListener", NONE);
		GLOBAL.addFunction("attachEvent", NONE);
		GLOBAL.addFunction("detachEvent", NONE);

		LOCATION.addProperty("href", STRING, Likelihood.YES, "navigates to new page");
		LOCATION.addFunction("replace", NONE, Likelihood.YES, "navigates to new page");
		LOCATION.addFunction("reload", NONE, Likelihood.YES, "navigates to new page");
		
		ARRAY.addFunction("concat", NONE);
		ARRAY.addFunction("join", NONE);
		ARRAY.addFunction("pop", NONE);
		ARRAY.addFunction("push", NONE);
		ARRAY.addFunction("reverse", NONE);
		ARRAY.addFunction("shift", NONE);
		ARRAY.addFunction("slice", NONE);
		ARRAY.addFunction("sort", NONE);
		ARRAY.addFunction("splice", NONE);
		ARRAY.addFunction("toString", NONE);
		ARRAY.addFunction("unshift", NONE);
		ARRAY.addFunction("valueOf", NONE);

		// These are only true for certain numbers of arguments
		JQUERY.addFunction("attr", JQUERY, Likelihood.YES, "attribute may not affect output", 2);
		JQUERY.addFunction("val", JQUERY, Likelihood.YES, "affects value of control", 2);
		JQUERY.addFunction("css", JQUERY, Likelihood.YES, "modifies style", 2);
		JQUERY.addFunction("text", JQUERY, Likelihood.YES, "modifies DOM", 1);
		JQUERY.addFunction("width", JQUERY, Likelihood.YES, "modifies DOM", 1);
		JQUERY.addFunction("height", JQUERY, Likelihood.YES, "modifies DOM", 1);
		JQUERY.addFunction("focus", JQUERY, Likelihood.YES, "modifies focus", 0);
		JQUERY.addFunction("select", JQUERY, Likelihood.YES, "changes text selection", 0);

		// composite CSS modification
		JQUERY.addFunction("removeAttr", JQUERY, Likelihood.YES, "attribute may not affect output");
		JQUERY.addFunction("animate", JQUERY, Likelihood.YES, "affects CSS");
		JQUERY.addFunction("fadeIn", JQUERY, Likelihood.YES, "affects CSS");
		JQUERY.addFunction("fadeOut", JQUERY, Likelihood.YES, "affects CSS");
		JQUERY.addFunction("fadeTo", JQUERY, Likelihood.YES, "affects CSS");
		JQUERY.addFunction("slideDown", JQUERY, Likelihood.YES, "affects CSS");
		JQUERY.addFunction("slideUp", JQUERY, Likelihood.YES, "affects CSS");
		JQUERY.addFunction("empty", JQUERY, Likelihood.YES, "affects DOM");
		JQUERY.addFunction("hide", JQUERY, Likelihood.YES, "may already be hidden");
		JQUERY.addFunction("show", JQUERY, Likelihood.YES, "may already be shown");
		JQUERY.addFunction("scrollTo", JQUERY, Likelihood.YES, "affects CSS");
		JQUERY.addFunction("slideToggle", JQUERY, Likelihood.YES, "affects CSS");
		JQUERY.addFunction("after", JQUERY, Likelihood.YES, "affects DOM");
		JQUERY.addFunction("before", JQUERY, Likelihood.YES, "affects DOM");
		JQUERY.addFunction("insertAfter", JQUERY, Likelihood.YES, "affects DOM");
		JQUERY.addFunction("insertBefore", JQUERY, Likelihood.YES, "affects DOM");
		JQUERY.addFunction("append", JQUERY, Likelihood.YES, "affects DOM");
		JQUERY.addFunction("appendTo", JQUERY, Likelihood.YES, "affects DOM");
		JQUERY.addFunction("prepend", JQUERY, Likelihood.YES, "affects DOM");
		JQUERY.addFunction("prependTo", JQUERY, Likelihood.YES, "affects DOM");
		JQUERY.addFunction("remove", JQUERY, Likelihood.YES, "affects DOM");
		JQUERY.addFunction("replaceAll", JQUERY, Likelihood.YES, "affects DOM");
		JQUERY.addFunction("replaceWith", JQUERY, Likelihood.YES, "affects DOM");
		JQUERY.addFunction("submit", JQUERY, Likelihood.YES, "submits form");
		JQUERY.addFunction("unwrap", JQUERY, Likelihood.YES, "affects DOM");
		JQUERY.addFunction("wrapAll", JQUERY, Likelihood.YES, "affects DOM");
		JQUERY.addFunction("wrapInner", JQUERY, Likelihood.YES, "affects DOM");
		JQUERY.addFunction("dialog", JQUERY, Likelihood.YES, "affects DOM");
		JQUERY.addFunction("addClass", JQUERY, Likelihood.YES, "may affect appearance");
		JQUERY.addFunction("removeClass", JQUERY, Likelihood.YES, "may affect appearance");
		
		// Need this for proper type propagation.
		JQUERY.addFunction("find", JQUERY);
		JQUERY.addFunction("not", JQUERY);
		JQUERY.addFunction("parent", JQUERY);
		JQUERY.addFunction("parents", JQUERY);
		
		// All style properties affect output (assuming the node is visible!).
		STYLE.addProperty("background", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("backgroundAttachment", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("backgroundColor", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("backgroundImage", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("backgroundPosition", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("backgroundRepeat", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("border", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("borderBottom", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("borderBottomColor", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("borderBottomStyle", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("borderBottomWidth", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("borderColor", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("borderLeft", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("borderLeftColor", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("borderLeftStyle", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("borderLeftWidth", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("borderRight", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("borderRightColor", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("borderRightStyle", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("borderRightWidth", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("borderStyle", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("borderTop", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("borderTopColor", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("borderTopStyle", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("borderTopWidth", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("borderWidth", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("clear", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("clip", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("color", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("cursor", STRING, Likelihood.YES, "may already be set to this value");
		STYLE.addProperty("display", STRING, Likelihood.YES, "may already be set to this value");
		STYLE.addProperty("filter", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("font", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("fontFamily", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("fontSize", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("fontVariant", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("fontWeight", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("height", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("left", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("letterSpacing", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("lineHeight", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("listStyle", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("listStyleImage", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("listStylePosition", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("listStyleType", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("margin", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("marginBottom", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("marginLeft", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("marginRight", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("marginTop", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("overflow", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("padding", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("paddingBottom", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("paddingLeft", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("paddingRight", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("paddingTop", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("pageBreakAfter", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("pageBreakBefore", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("position", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("float", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("textAlign", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("textDecoration", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("textIndent", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("textTransform", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("top", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("verticalAlign", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("visibility", STRING, Likelihood.YES, "may already be set to this value");
		STYLE.addProperty("width", STRING, Likelihood.YES, "affects style");
		STYLE.addProperty("zIndex", STRING, Likelihood.YES, "affects style");
		
		CONTEXT2D.addProperty("fillStyle", STRING, Likelihood.YES, "affects fill patterns");
		CONTEXT2D.addProperty("strokeStyle", STRING, Likelihood.YES, "affects fill patterns");
		CONTEXT2D.addProperty("strokeWidth", STRING, Likelihood.YES, "affects line appearance");
		CONTEXT2D.addProperty("lineWidth", NUMBER, Likelihood.YES, "affects line appearance");
		CONTEXT2D.addProperty("font", STRING, Likelihood.YES, "affects text appearance");
		CONTEXT2D.addProperty("textBaseline", NUMBER, Likelihood.YES, "affects text appearance");
		CONTEXT2D.addProperty("shadowOffset", STRING, Likelihood.YES, "affects text appearance");
		CONTEXT2D.addProperty("shadowOffsetX", NUMBER, Likelihood.YES, "affects text appearance");
		CONTEXT2D.addProperty("shadowOffsetY", NUMBER, Likelihood.YES, "affects text appearance");
		CONTEXT2D.addProperty("shadowBlur", STRING, Likelihood.YES, "affects text appearance");
		CONTEXT2D.addProperty("globalAlpha", STRING, Likelihood.YES, "affects text appearance");
		CONTEXT2D.addProperty("globalCompositeOperation", STRING, Likelihood.YES, "affects text appearance");

		CONTEXT2D.addFunction("save", NONE, Likelihood.NO, "saves context");
		CONTEXT2D.addFunction("restore", NONE, Likelihood.NO, "restores context");
		
		CONTEXT2D.addFunction("arc", NONE, Likelihood.YES, "draws arc");
		CONTEXT2D.addFunction("fillRect", NONE, Likelihood.YES, "draws a rectangle");
		CONTEXT2D.addFunction("strokeRect", NONE, Likelihood.YES, "draws a rectangle");
		CONTEXT2D.addFunction("clearRect", NONE, Likelihood.YES, "clears a rectangle");
		CONTEXT2D.addFunction("moveTo", NONE, Likelihood.YES, "modifies drawing position");
		CONTEXT2D.addFunction("lineTo", NONE, Likelihood.YES, "modifies drawing position");
		CONTEXT2D.addFunction("fill", NONE, Likelihood.YES, "fills shape");
		CONTEXT2D.addFunction("stroke", NONE, Likelihood.YES, "draws shape");
		CONTEXT2D.addFunction("beginPath", NONE, Likelihood.YES, "starts shape");
		CONTEXT2D.addFunction("closePath", NONE, Likelihood.YES, "finishes shape");
		CONTEXT2D.addFunction("drawImage", NONE, Likelihood.YES, "draws image");
		CONTEXT2D.addFunction("putImageData", NONE, Likelihood.YES, "draws image");
		CONTEXT2D.addFunction("fillText", NONE, Likelihood.YES, "draws text");
		CONTEXT2D.addFunction("strokeText", NONE, Likelihood.YES, "draws text");
		CONTEXT2D.addFunction("bezierCurveTo", NONE, Likelihood.YES, "draws curve");
		CONTEXT2D.addFunction("translate", NONE, Likelihood.YES, "modifies drawing position");
		CONTEXT2D.addFunction("rotate", NONE, Likelihood.YES, "modifies drawing position");
		
		// Date methods
//		"getDate",
//		"getDay",
//		"getFullYear",
//		"getHours",
//		"getMilliseconds",
//		"getMinutes",
//		"getMonth",
//		"getSeconds",
//		"getTime",
//		"getTimezoneOffset",
//		"getUTCDate",
//		"getUTCDay",
//		"getUTCFullYear",
//		"getUTCHours",
//		"getUTCMilliseconds",
//		"getUTCMinutes",
//		"getUTCMonth",
//		"getUTCSeconds",
//		"getYear",
//		"parse",
//		"setDate",
//		"setFullYear",
//		"setHours",
//		"setMilliseconds",
//		"setMinutes",
//		"setMonth",
//		"setSeconds",
//		"setTime",
//		"setUTCDate",
//		"setUTCFullYear",
//		"setUTCHours",
//		"setUTCMilliseconds",
//		"setUTCMinutes",
//		"setUTCMonth",
//		"setUTCSeconds",
//		"setYear",
//		"toDateString",
//		"toGMTString",
//		"toLocaleDateString",
//		"toLocaleTimeString",
//		"toLocaleString",
//		"toTimeString",
//		"toUTCString",
//		"UTC",
				
		// Math		
//		"abs",
//		"acos",
//		"asin",
//		"atan",
//		"atan2",
//		"ceil",
//		"cos",
//		"exp",
//		"floor",
//		"log",
//		"max",
//		"min",
//		"pow",
//		"random",
//		"round",
//		"sin",
//		"sqrt",
//		"tan",
				
		// Number		
//		"toExponential",
//		"toFixed",
//		"toPrecision",
				
		// String		
//		"charAt",
//		"charCodeAt",
//		"concat",
//		"fromCharCode",
//		"indexOf",
//		"lastIndexOf",
//		"match",
//		"replace",
//		"search",
//		"slice",
//		"split",
//		"substr",
//		"substring",
//		"toLowerCase",
//		"toUpperCase",
//		"anchor",
//		"big",
//		"blink",
//		"bold",
//		"fixed",
//		"fontcolor",
//		"fontsize",
//		"italics",
//		"link",
//		"small",
//		"strike",
//		"sub",
//		"sup",
				
		// Regex
//		"compile",
//		"exec",
//		"test",
				
		// History
//		"back",
//		"forward",
//		"go",
				
		// Location		
//		"assign",
//		"reload",
//		"replace",
				
		// Node
//		"hasChildNodes",
//		"cloneNode",
//		"normalize",
//		"isSupported",
//		"hasAttributes",
//		"getAttribute",
//		"compareDocumentPosition",
//		"isSameNode",
//		"lookupPrefix",
//		"isDefaultNamespace",
//		"lookupNamespaceURI",
//		"isEqualNode",
//		"getFeature",
//		"setUserData",
//		"getUserData",
		
		// Misc
		
		// Event
//		"stopPropagation",
//		"preventDefault",
				
	};
	
	private static final String[] CSS_ATTRIBUTES = {
		
		"background", 				"object",
		"background-attachment", 	"enum",
		"background-color",			"color",
		"background-image", 		"url",
		"background-position", 		"object",
		"background-repeat", 		"enum",
		"border", 					"object",
		"border-bottom", 			"object",
		"border-bottom-color", 		"color",
		"border-bottom-style", 		"enum",
		"border-bottom-width", 		"value",
		"border-color", 			"color",
		"border-left",			 	"object",
		"border-left-color", 		"color",
		"border-left-style",		"enum",
		"border-left-width", 		"value",
		"border-right", 			"object",
		"border-right-color", 		"color",
		"border-right-style",		"enum",
		"border-right-width", 		"value",
		"border-style", 			"enum",
		"border-top", 				"object",
		"border-top-color", 		"color",
		"border-top-style", 		"enum",
		"border-top-width", 		"value",
		"border-width", 			"value",
		"clear",					"enum",
		"clip", 					"object",
		"color", 					"color",
		"cursor",					"enum",
		"display",					"enum",
		"filter",					"url",
		"font", 					"object",
		"font-family", 				"enum",
		"font-size", 				"value",
		"font-variant", 			"enum",
		"font-weight",				"value",
		"height",					"value",
		"left",						"value",
		"letter-spacing",			"value",
		"line-height",				"value",
		"list-style",				"enum",
		"list-style-image",			"url",
		"list-style-position",		"enum",
		"list-style-type",			"enum",
		"margin",					"object",
		"margin-bottom",			"value",
		"margin-left",				"value",
		"margin-right",				"value",
		"margin-top",				"value",
		"overflow",					"enum",
		"padding",					"object",
		"padding-bottom",			"value",
		"padding-left",				"value",
		"padding-right",			"value",
		"padding-top",				"value",
		"page-break-after",			"enum",
		"page-break-before",		"enum",
		"position",					"enum",
		"float",					"enum",
		"text-align",				"enum",
		"text-decoration",			"enum",
		"text-indent",				"value",
		"text-transform",			"enum",
		"top",						"value",
		"vertical-align",			"enum",
		"visibility",				"enum",
		"width",					"value",
		"z-index",					"value",
	};
	
	private static final Map<String,String> CSS_ATTRIBUTE_TYPES_BY_CSS_NAME = new Hashtable<String,String>();
	private static final Map<String,String> CSS_ATTRIBUTE_TYPES_BY_JS_NAME = new Hashtable<String,String>();
	
	static {
		
		assert CSS_ATTRIBUTES.length % 2 == 0;
		for(int i = 0; i < CSS_ATTRIBUTES.length; i+=2) {
			
			String name = CSS_ATTRIBUTES[i];
			String type = CSS_ATTRIBUTES[i + 1];
			
			CSS_ATTRIBUTE_TYPES_BY_CSS_NAME.put(name, type);
			
			StringBuilder jsName = new StringBuilder();
			char[] chars = name.toCharArray();
			boolean capNext = false;
			for(char c : chars) {
				
				if(c == '-') capNext = true;
				else {
					if(capNext) {
						capNext = false;
						c = Character.toUpperCase(c);
					}
					jsName.append(c);
				}
				
			}

			CSS_ATTRIBUTE_TYPES_BY_JS_NAME.put(jsName.toString(), type);
			
		}
		
	}
	
	/**
	 * These are event names that Feedlack considers user input deserving of feedback.
	 */
	private static final Set<String> INPUT_EVENTS_REQUIRING_OUTPUT = new HashSet<String>();
	
	static {
		
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("activate");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("beforecopy");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("beforecut");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("beforedeactivate");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("beforeeditfocus");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("beforepaste");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("blur");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("click");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("contextmenu");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("click");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("controlselect");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("copy");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("cut");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("dblclick");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("deactivate");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("drag");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("dragend");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("dragenter");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("dragleave");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("dragover");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("dragstart");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("drop");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("filterchange");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("focus");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("focusin");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("focusout");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("help");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("keydown");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("keypress");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("keyup");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("losecapture");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("mousedown");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("mouseenter");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("mousemove");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("mouseout");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("mouseover");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("mouseup");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("mousewheel");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("move");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("moveend");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("movestart");
		INPUT_EVENTS_REQUIRING_OUTPUT.add("paste");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("propertychange");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("resize");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("resizeend");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("resizestart");
//		INPUT_EVENTS_REQUIRING_OUTPUT.add("selectstart");		
		INPUT_EVENTS_REQUIRING_OUTPUT.add("submit");
		
	}

	/**
	 * These are explicit event binding calls supported by jQuery, extracted from the jQuery API.
	 */
	private static final Set<String> JQUERY_EVENT_NAMES = new HashSet<String>();
	
	static {
		
		JQUERY_EVENT_NAMES.add("bind");
		JQUERY_EVENT_NAMES.add("one");
		JQUERY_EVENT_NAMES.add("toggle");
		JQUERY_EVENT_NAMES.add("change");
		JQUERY_EVENT_NAMES.add("click");
		JQUERY_EVENT_NAMES.add("dblclick");
		JQUERY_EVENT_NAMES.add("error");
		JQUERY_EVENT_NAMES.add("focus");
		JQUERY_EVENT_NAMES.add("focusin");
		JQUERY_EVENT_NAMES.add("focusout");
		JQUERY_EVENT_NAMES.add("hover");
		JQUERY_EVENT_NAMES.add("keydown");
		JQUERY_EVENT_NAMES.add("keypress");
		JQUERY_EVENT_NAMES.add("keyup");
		JQUERY_EVENT_NAMES.add("mousedown");
		JQUERY_EVENT_NAMES.add("mouseenter");
		JQUERY_EVENT_NAMES.add("mouseleave");
		JQUERY_EVENT_NAMES.add("mousemove");
		JQUERY_EVENT_NAMES.add("mouseout");
		JQUERY_EVENT_NAMES.add("mouseover");
		JQUERY_EVENT_NAMES.add("mouseup");
		JQUERY_EVENT_NAMES.add("mouseup");
		JQUERY_EVENT_NAMES.add("resize");
		JQUERY_EVENT_NAMES.add("scroll");
		JQUERY_EVENT_NAMES.add("select");
		
	}

	
}
