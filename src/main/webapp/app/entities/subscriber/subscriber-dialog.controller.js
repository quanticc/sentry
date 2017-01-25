(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('SubscriberDialogController', SubscriberDialogController);

    SubscriberDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Subscriber', 'ParseMaps'];

    function SubscriberDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Subscriber, ParseMaps) {
        var vm = this;

        vm.outputTypes = ['DiscordMessage', 'DiscordWebhook', 'DatadogEvent', 'DiscordEmbed', 'DiscordMessageEmbed'];
        vm.subscriber = entity;
        vm.entries = ParseMaps.parseToEntries(vm.subscriber.variables);
        vm.clear = clear;
        vm.save = save;

        $timeout(function (){
            angular.element('.form-group:eq(1)>input').focus();
        });

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function save () {
            vm.isSaving = true;
            vm.subscriber.variables = ParseMaps.parseToMap(vm.entries);
            if (vm.subscriber.id !== null) {
                Subscriber.update(vm.subscriber, onSaveSuccess, onSaveError);
            } else {
                Subscriber.save(vm.subscriber, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sentryApp:subscriberUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }

    }
})();
