(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('FlowDialogController', FlowDialogController);

    FlowDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Flow', 'ParseMaps'];

    function FlowDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Flow, ParseMaps) {
        var vm = this;

        vm.inputTypes = ['sentryEvent', 'inboundWebhook'];
        vm.translatorTypes = ['DiscordMessage', 'DiscordWebhook', 'DatadogEvent', 'DatadogDowntime', 'DiscordEmbed', 'DiscordMessageEmbed'];
        vm.flow = entity;
        vm.entries = ParseMaps.parseToEntries(vm.flow.variables);
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
            vm.flow.variables = ParseMaps.parseToMap(vm.entries);
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
