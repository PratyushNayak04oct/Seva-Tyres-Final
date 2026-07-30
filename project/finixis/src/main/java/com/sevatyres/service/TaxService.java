package com.sevatyres.service;

import com.sevatyres.model.Tax;
import com.sevatyres.repository.TaxRepository;
import com.sevatyres.repository.impl.JdbcTaxRepository;

import java.util.List;
import java.util.Optional;

public class TaxService {

    private final TaxRepository repo = new JdbcTaxRepository();

    public List<Tax> getAll() { return repo.findAll(); }
    public List<Tax> getActive() { return repo.findActive(); }
    public Optional<Tax> getById(int id) { return repo.findById(id); }

    public Tax save(Tax tax) {
        if (tax.getName() == null || tax.getName().isBlank()) {
            throw new IllegalArgumentException("Tax name is required.");
        }
        if (tax.getRate() < 0) {
            throw new IllegalArgumentException("Tax rate cannot be negative.");
        }
        if (tax.getId() > 0) {
            repo.update(tax);
            return tax;
        }
        return repo.save(tax);
    }

    public void delete(int id) { repo.delete(id); }

    /** Compute tax amount for a subtotal given a list of taxes. */
    public static double computeTaxAmount(double subtotal, List<Tax> taxes) {
        if (taxes == null || taxes.isEmpty() || subtotal <= 0) return 0;
        double sum = 0;
        for (Tax t : taxes) {
            if (t != null && t.getRate() > 0) sum += subtotal * (t.getRate() / 100.0);
        }
        return Math.round(sum * 100.0) / 100.0;
    }

    public static String buildTaxLabel(List<Tax> taxes) {
        if (taxes == null || taxes.isEmpty()) return "No tax";
        StringBuilder sb = new StringBuilder();
        for (Tax t : taxes) {
            if (t == null) continue;
            if (!sb.isEmpty()) sb.append(" + ");
            sb.append(t.getDisplayLabel());
        }
        return sb.isEmpty() ? "No tax" : sb.toString();
    }
}
