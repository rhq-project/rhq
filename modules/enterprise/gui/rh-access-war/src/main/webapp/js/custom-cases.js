angular.module('RedhatAccess.customCaseView', ['RedhatAccess.cases'])
.controller('customCase', ['$scope', '$location', 'securityService', 'NEW_DEFAULTS', function($scope, $location, securityService, NEW_DEFAULTS) {
	NEW_DEFAULTS.product = "Red Hat JBoss Operations Network";
	NEW_DEFAULTS.version = "3.3.0";
	 $scope.selected = 'open-case';
	 $scope.openCaseClick = function(){
		 $location.path('case/new');
	 };
	 $scope.modifyCaseClick = function(){
		 $location.path('case/compact');
	 };
	 $scope.init = function () {
		 securityService.validateLogin(true);
		 if($location.$$path == '/case/compact'){
			 $scope.selected = 'modify-case';
		 } else{
			 $location.path('case/new');
		 }
	 };
}]);