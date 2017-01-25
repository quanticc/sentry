(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('FlowDialogController', FlowDialogController);

    FlowDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Flow'];

    function FlowDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Flow) {
        var vm = this;

        vm.inputTypes = ['sentryEvent', 'inboundWebhook'];
        vm.translatorTypes = ['DiscordMessage', 'DiscordWebhook', 'DatadogEvent', 'DiscordEmbed', 'DiscordMessageEmbed'];
        vm.flow = entity;
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
            if (vm.flow.id !== null) {
                Flow.update(vm.flow, onSaveSuccess, onSaveError);
            } else {
                Flow.save(vm.flow, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sentryApp:flowUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
