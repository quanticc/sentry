(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('SubscriberDetailController', SubscriberDetailController);

    SubscriberDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Subscriber'];

    function SubscriberDetailController($scope, $rootScope, $stateParams, previousState, entity, Subscriber) {
        var vm = this;

        vm.subscriber = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sentryApp:subscriberUpdate', function(event, result) {
            vm.subscriber = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
