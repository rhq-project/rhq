var attachmentsRequest = 'attachments';

angular.module('RedhatAccess.JON', ['RedhatAccess.cases'])
.controller('customCase', ['$scope', 'securityService', 'NEW_DEFAULTS', '$location', function($scope, securityService, NEW_DEFAULTS , $location) {
  NEW_DEFAULTS.product = "Red Hat JBoss Operations Network";
  NEW_DEFAULTS.version = "3.3.0";
  
  // handle support case for managed resource
  if ($location.path().indexOf('/resource-case') >= 0) {
    var params = $location.search();
    NEW_DEFAULTS.product = params.product;
    NEW_DEFAULTS.version = params.version;
    attachmentsRequest += '?resourceId='+params.resourceId;
    $location.path('/case/new')
  }

   $scope.init = function () {
     securityService.validateLogin(true);
   };
}]);
angular.module('RedhatAccess.cases')
  .controller('BackEndAttachmentsCtrl', ['$scope', 'TreeViewSelectorData', 'AttachmentsService',
    function ($scope, TreeViewSelectorData, AttachmentsService) {
      $scope.name = 'Attachments';
      $scope.attachmentTree = [];
      TreeViewSelectorData.getTree(attachmentsRequest).then(
        function (tree) {
          $scope.attachmentTree = tree;
          AttachmentsService.updateBackEndAttachments(tree);
        },
        function () {
          console.log('Unable to get tree data');
        });
    }
  ]);
  