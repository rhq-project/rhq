exports.nest = function() {
	return "Deep nest";
}

exports.maze = function() {
	var m1 = require('../test-module1');
	return m1.func1();
}
