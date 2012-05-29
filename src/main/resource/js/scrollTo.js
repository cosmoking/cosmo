	
/**
 * jQuery.ScrollTo
 * Copyright (c) 2007-2009 Ariel Flesler - aflesler(at)gmail(dot)com | http://flesler.blogspot.com
 * Dual licensed under MIT and GPL.
 * Date: 5/25/2009
 *
 * @projectDescription Easy element scrolling using jQuery.
 * http://flesler.blogspot.com/2007/10/jqueryscrollto.html
 * Works with jQuery +1.2.6. Tested on FF 2/3, IE 6/7/8, Opera 9.5/6, Safari 3, Chrome 1 on WinXP.
 *
 * @author Ariel Flesler
 * @version 1.4.2
 *
 * @id jQuery.scrollTo
 * @id jQuery.fn.scrollTo
 * @param {String, Number, DOMElement, jQuery, Object} target Where to scroll the matched elements.
 *	  The different options for target are:
 *		- A number position (will be applied to all axes).
 *		- A string position ('44', '100px', '+=90', etc ) will be applied to all axes
 *		- A jQuery/DOM element ( logically, child of the element to scroll )
 *		- A string selector, that will be relative to the element to scroll ( 'li:eq(2)', etc )
 *		- A hash { top:x, left:y }, x and y can be any kind of number/string like above.
*		- A percentage of the container's dimension/s, for example: 50% to go to the middle.
 *		- The string 'max' for go-to-end. 
 * @param {Number} duration The OVERALL length of the animation, this argument can be the settings object instead.
 * @param {Object,Function} settings Optional set of settings or the onAfter callback.
 *	 @option {String} axis Which axis must be scrolled, use 'x', 'y', 'xy' or 'yx'.
 *	 @option {Number} duration The OVERALL length of the animation.
 *	 @option {String} easing The easing method for the animation.
 *	 @option {Boolean} margin If true, the margin of the target element will be deducted from the final position.
 *	 @option {Object, Number} offset Add/deduct from the end position. One number for both axes or { top:x, left:y }.
 *	 @option {Object, Number} over Add/deduct the height/width multiplied by 'over', can be { top:x, left:y } when using both axes.
 *	 @option {Boolean} queue If true, and both axis are given, the 2nd axis will only be animated after the first one ends.
 *	 @option {Function} onAfter Function to be called after the scrolling ends. 
 *	 @option {Function} onAfterFirst If queuing is activated, this function will be called after the first scrolling ends.
 * @return {jQuery} Returns the same jQuery object, for chaining.
 *
 * @desc Scroll to a fixed position
 * @example $('div').scrollTo( 340 );
 *
 * @desc Scroll relatively to the actual position
 * @example $('div').scrollTo( '+=340px', { axis:'y' } );
 *
 * @dec Scroll using a selector (relative to the scrolled element)
 * @example $('div').scrollTo( 'p.paragraph:eq(2)', 500, { easing:'swing', queue:true, axis:'xy' } );
 *
 * @ Scroll to a DOM element (same for jQuery object)
 * @example var second_child = document.getElementById('container').firstChild.nextSibling;
 *			$('#container').scrollTo( second_child, { duration:500, axis:'x', onAfter:function(){
 *				alert('scrolled!!');																   
 *			}});
 *
 * @desc Scroll on both axes, to different values
 * @example $('div').scrollTo( { top: 300, left:'+=200' }, { axis:'xy', offset:-20 } );
 */
;(function( $ ){
	
	var $scrollTo = $.scrollTo = function( target, duration, settings ){
		$(window).scrollTo( target, duration, settings );
	};

	$scrollTo.defaults = {
		axis:'xy',
		duration: parseFloat($.fn.jquery) >= 1.3 ? 0 : 1
	};

	// Returns the element that needs to be animated to scroll the window.
	// Kept for backwards compatibility (specially for localScroll & serialScroll)
	$scrollTo.window = function( scope ){
		return $(window)._scrollable();
	};

	// Hack, hack, hack :)
	// Returns the real elements to scroll (supports window/iframes, documents and regular nodes)
	$.fn._scrollable = function(){
		return this.map(function(){
			var elem = this,
				isWin = !elem.nodeName || $.inArray( elem.nodeName.toLowerCase(), ['iframe','#document','html','body'] ) != -1;

				if( !isWin )
					return elem;

			var doc = (elem.contentWindow || elem).document || elem.ownerDocument || elem;
			
			return $.browser.safari || doc.compatMode == 'BackCompat' ?
				doc.body : 
				doc.documentElement;
		});
	};

	$.fn.scrollTo = function( target, duration, settings ){
		if( typeof duration == 'object' ){
			settings = duration;
			duration = 0;
		}
		if( typeof settings == 'function' )
			settings = { onAfter:settings };
			
		if( target == 'max' )
			target = 9e9;
			
		settings = $.extend( {}, $scrollTo.defaults, settings );
		// Speed is still recognized for backwards compatibility
		duration = duration || settings.speed || settings.duration;
		// Make sure the settings are given right
		settings.queue = settings.queue && settings.axis.length > 1;
		
		if( settings.queue )
			// Let's keep the overall duration
			duration /= 2;
		settings.offset = both( settings.offset );
		settings.over = both( settings.over );

		return this._scrollable().each(function(){
			var elem = this,
				$elem = $(elem),
				targ = target, toff, attr = {},
				win = $elem.is('html,body');

			switch( typeof targ ){
				// A number will pass the regex
				case 'number':
				case 'string':
					if( /^([+-]=)?\d+(\.\d+)?(px|%)?$/.test(targ) ){
						targ = both( targ );
						// We are done
						break;
					}
					// Relative selector, no break!
					targ = $(targ,this);
				case 'object':
					// DOMElement / jQuery
					if( targ.is || targ.style )
						// Get the real position of the target 
						toff = (targ = $(targ)).offset();
			}
			$.each( settings.axis.split(''), function( i, axis ){
				var Pos	= axis == 'x' ? 'Left' : 'Top',
					pos = Pos.toLowerCase(),
					key = 'scroll' + Pos,
					old = elem[key],
					max = $scrollTo.max(elem, axis);

				if( toff ){// jQuery / DOMElement
					attr[key] = toff[pos] + ( win ? 0 : old - $elem.offset()[pos] );

					// If it's a dom element, reduce the margin
					if( settings.margin ){
						attr[key] -= parseInt(targ.css('margin'+Pos)) || 0;
						attr[key] -= parseInt(targ.css('border'+Pos+'Width')) || 0;
					}
					
					attr[key] += settings.offset[pos] || 0;
					
					if( settings.over[pos] )
						// Scroll to a fraction of its width/height
						attr[key] += targ[axis=='x'?'width':'height']() * settings.over[pos];
				}else{ 
					var val = targ[pos];
					// Handle percentage values
					attr[key] = val.slice && val.slice(-1) == '%' ? 
						parseFloat(val) / 100 * max
						: val;
				}

				// Number or 'number'
				if( /^\d+$/.test(attr[key]) )
					// Check the limits
					attr[key] = attr[key] <= 0 ? 0 : Math.min( attr[key], max );

				// Queueing axes
				if( !i && settings.queue ){
					// Don't waste time animating, if there's no need.
					if( old != attr[key] )
						// Intermediate animation
						animate( settings.onAfterFirst );
					// Don't animate this axis again in the next iteration.
					delete attr[key];
				}
			});

			animate( settings.onAfter );			

			function animate( callback ){
				$elem.animate( attr, duration, settings.easing, callback && function(){
					callback.call(this, target, settings);
				});
			};

		}).end();
	};
	
	// Max scrolling position, works on quirks mode
	// It only fails (not too badly) on IE, quirks mode.
	$scrollTo.max = function( elem, axis ){
		var Dim = axis == 'x' ? 'Width' : 'Height',
			scroll = 'scroll'+Dim;
		
		if( !$(elem).is('html,body') )
			return elem[scroll] - $(elem)[Dim.toLowerCase()]();
		
		var size = 'client' + Dim,
			html = elem.ownerDocument.documentElement,
			body = elem.ownerDocument.body;

		return Math.max( html[scroll], body[scroll] ) 
			 - Math.min( html[size]  , body[size]   );
			
	};

	function both( val ){
		return typeof val == 'object' ? val : { top:val, left:val };
	};

})( jQuery );		


/*
 * jQuery.SerialScroll
 * Copyright (c) 2007-2008 Ariel Flesler - aflesler(at)gmail(dot)com | http://flesler.blogspot.com
 * Dual licensed under MIT and GPL.
 * Date: 06/14/2009
 *
 * @projectDescription Animated scrolling of series.
 * @author Ariel Flesler
 * @version 1.2.2
 *
 * @id jQuery.serialScroll
 * @id jQuery.fn.serialScroll
 * @param {Object} settings Hash of settings, it is passed in to jQuery.ScrollTo, none is required.
 * @return {jQuery} Returns the same jQuery object, for chaining.
 *
 * @link {http://flesler.blogspot.com/2008/02/jqueryserialscroll.html Homepage}
 *
 * Notes:
 *	- The plugin requires jQuery.ScrollTo.
 *	- The hash of settings, is passed to jQuery.ScrollTo, so its settings can be used as well.
 */
;(function( $ ){

	var $serialScroll = $.serialScroll = function( settings ){
		return $(window).serialScroll( settings );
	};

	// Many of these defaults, belong to jQuery.ScrollTo, check it's demo for an example of each option.
	// @link {http://demos.flesler.com/jquery/scrollTo/ ScrollTo's Demo}
	$serialScroll.defaults = {// the defaults are public and can be overriden.
		duration:1000, // how long to animate.
		axis:'x', // which of top and left should be scrolled
		event:'click', // on which event to react.
		start:0, // first element (zero-based index)
		step:1, // how many elements to scroll on each action
		lock:true,// ignore events if already animating
		cycle:true, // cycle endlessly ( constant velocity )
		constant:true // use contant speed ?
		/*
		navigation:null,// if specified, it's a selector a collection of items to navigate the container
		target:window, // if specified, it's a selector to the element to be scrolled.
		interval:0, // it's the number of milliseconds to automatically go to the next
		lazy:false,// go find the elements each time (allows AJAX or JS content, or reordering)
		stop:false, // stop any previous animations to avoid queueing
		force:false,// force the scroll to the first element on start ?
		jump: false,// if true, when the event is triggered on an element, the pane scrolls to it
		items:null, // selector to the items (relative to the matched elements)
		prev:null, // selector to the 'prev' button
		next:null, // selector to the 'next' button
		onBefore: function(){}, // function called before scrolling, if it returns false, the event is ignored
		exclude:0 // exclude the last x elements, so we cannot scroll past the end
		*/
	};

	$.fn.serialScroll = function( options ){

		return this.each(function(){
			var 
				settings = $.extend( {}, $serialScroll.defaults, options ),
				event = settings.event, // this one is just to get shorter code when compressed
				step = settings.step, // ditto
				lazy = settings.lazy, // ditto
				context = settings.target ? this : document, // if a target is specified, then everything's relative to 'this'.
				$pane = $(settings.target || this, context),// the element to be scrolled (will carry all the events)
				pane = $pane[0], // will be reused, save it into a variable
				items = settings.items, // will hold a lazy list of elements
				active = settings.start, // active index
				auto = settings.interval, // boolean, do auto or not
				nav = settings.navigation, // save it now to make the code shorter
				timer; // holds the interval id

			if( !lazy )// if not lazy, save the items now
				items = getItems();

			if( settings.force )
				jump( {}, active );// generate an initial call

			// Button binding, optional
			$(settings.prev||[], context).bind( event, -step, move );
			$(settings.next||[], context).bind( event, step, move );

			// Custom events bound to the container
			if( !pane.ssbound )// don't bind more than once
				$pane
					.bind('prev.serialScroll', -step, move ) // you can trigger with just 'prev'
					.bind('next.serialScroll', step, move ) // f.e: $(container).trigger('next');
					.bind('goto.serialScroll', jump ); // f.e: $(container).trigger('goto', 4 );

			if( auto )
				$pane
					.bind('start.serialScroll', function(e){
						if( !auto ){
							clear();
							auto = true;
							next();
						}
					 })
					.bind('stop.serialScroll', function(){// stop a current animation
						clear();
						auto = false;
					});

			$pane.bind('notify.serialScroll', function(e, elem){// let serialScroll know that the index changed externally
				var i = index(elem);
				if( i > -1 )
					active = i;
			});

			pane.ssbound = true;// avoid many bindings

			if( settings.jump )// can't use jump if using lazy items and a non-bubbling event
				(lazy ? $pane : getItems()).bind( event, function( e ){
					jump( e, index(e.target) );
				});

			if( nav )
				nav = $(nav, context).bind(event, function( e ){
					e.data = Math.round(getItems().length / nav.length) * nav.index(this);
					jump( e, this );
				});

			function move( e ){
				e.data += active;
				jump( e, this );
			};
			function jump( e, button ){
				if( !isNaN(button) ){// initial or special call from the outside $(container).trigger('goto',[index]);
					e.data = button;
					button = pane;
				}

				var
					pos = e.data, n,
					real = e.type, // is a real event triggering ?
					$items = settings.exclude ? getItems().slice(0,-settings.exclude) : getItems(),// handle a possible exclude
					limit = $items.length,
					elem = $items[pos],
					duration = settings.duration;

				if( real )// real event object
					e.preventDefault();

				if( auto ){
					clear();// clear any possible automatic scrolling.
					timer = setTimeout( next, settings.interval ); 
				}

				if( !elem ){ // exceeded the limits
					n = pos < 0 ? 0 : limit - 1;
					if( active != n )// we exceeded for the first time
						pos = n;
					else if( !settings.cycle )// this is a bad case
						return;
					else
						pos = limit - n - 1;// invert, go to the other side
					elem = $items[pos];
				}

				if( !elem || settings.lock && $pane.is(':animated') || // no animations while busy
					real && settings.onBefore &&
					settings.onBefore(e, elem, $pane, getItems(), pos) === false ) return;

				if( settings.stop )
					$pane.queue('fx',[]).stop();// remove all its animations

				if( settings.constant )
					duration = Math.abs(duration/step * (active - pos ));// keep constant velocity

				$pane
					.scrollTo( elem, duration, settings )// do scroll
					.trigger('notify.serialScroll',[pos]);// in case serialScroll was called on this elem more than once.
			};

			function next(){// I'll use the namespace to avoid conflicts
				$pane.trigger('next.serialScroll');
			};

			function clear(){
				clearTimeout(timer);
			};

			function getItems(){
				return $( items, pane );
			};

			function index( elem ){
				if( !isNaN(elem) ) return elem;// number
				var $items = getItems(), i;
				while(( i = $items.index(elem)) == -1 && elem != pane )// see if it matches or one of its ancestors
					elem = elem.parentNode;
				return i;
			};
		});
	};

})( jQuery );
		