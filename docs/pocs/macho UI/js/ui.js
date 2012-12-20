var ui = {
	stage : null,
	scale: 1,
	zoomFactor : 1.1,
	origin : { x : 0, y : 0},
	zoom : function(event) {
		event.preventDefault();
		var evt = event.originalEvent, 
			mx = evt.clientX - ui.stage.content.offsetLeft,
			my = evt.clientY - ui.stage.content.offsetTop,
			wheel = evt.wheelDelta / 120;//n or -n

		var zoom = (ui.zoomFactor - (evt.wheelDelta < 0 ? 0.2 : 0));
		var newscale = ui.scale * zoom;
		ui.origin.x = mx / ui.scale + ui.origin.x - mx / newscale;
		ui.origin.y = my / ui.scale + ui.origin.y - my / newscale;
		
		ui.stage.setOffset(ui.origin.x, ui.origin.y);
		ui.stage.setScale(newscale);
		ui.stage.draw();

		ui.scale *= zoom;
	},
	addNamespace : function() {
		var layer = new Kinetic.Layer({draggable : true});
		
		var ns = new Kinetic.Circle({
			x: 100,
			y: 100,
			radius: 50,
			fill: '#00D2CC',
			stroke: 'black',
			strokeWidth: 2
		});
		
		// add cursor styling
		ns.on('mouseover', function() {
			document.body.style.cursor = 'pointer';
		});
		ns.on('mouseout', function() {
			document.body.style.cursor = 'default';
		});

		layer.add(ns);
		ui.stage.add(layer);
		
		return layer;
	},
	addDefinition : function() {
		var def = new Kinetic.Circle({
			x: 100,
			y: 50 + (++d * 10),
			radius: 10,
			fill: '#11D2FF',
			stroke: '#CCC',
			strokeWidth: 1,
			draggable: true
		});
		
		def.on('mouseover', function() {
			document.body.style.cursor = 'pointer';
		});
		def.on('mouseout', function() {
			document.body.style.cursor = 'default';
		});

		ui.stage.off('click');
		ui.stage.on('click', function() {
			ui.stage.off('click');
			var pos = ui.stage.getMousePosition();
			var result = ui.stage.getIntersection(pos);
			result.shape.getParent().add(def);

		});
	}
};

var d = 0;

$(function() {
	var width = $(document).width() - 155, height = $(document).height() - 5;
    var stage = ui.stage = new Kinetic.Stage({
        container: 'workarea',
        width: width,
        height: height
    });
	
	$(stage.content).on('mousewheel', ui.zoom);	
	$('#options .add-ns').on('click', ui.addNamespace);
	$('#options .add-def').on('click', ui.addDefinition);
});